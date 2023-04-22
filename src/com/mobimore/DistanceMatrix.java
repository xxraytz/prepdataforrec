package com.mobimore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DistanceMatrix {

    private String path_database = "";
    private String path_video = "";
    private ArrayList<List<Joint>> skeletonVideoSequence = new ArrayList<>();
    private ArrayList<List<Joint>> baseAssembly = new ArrayList<>();

    public DistanceMatrix(String[] args) {
        //Получение аргументов
        path_database = args[0];
        String order_path = args[1];
        readSkeletonCSVfromTST(path_database, baseAssembly);

        if (order_path != null) {
            Integer[] order = new Integer[baseAssembly.size()];
            try {
                order = MatrixParameters.get_order(order_path);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            change_ordering(order);
            //Сортировка базисной совокупности
        }

        try {
            add_skeletons("./data/database_advanced/laying/", -1);
            add_skeletons("./data/database_advanced/grasp/", -1);
            add_skeletons("./data/database_advanced/stay_falling/", 32);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            cleanup("data/cleanup_seq.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void cleanup(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line = br.readLine();
        Integer[] cleanup_order = Arrays.stream(line.split(" ")).map(Integer::parseInt).toArray(Integer[]::new);
        this.baseAssembly =
                (ArrayList<List<Joint>>) Arrays.stream(cleanup_order).map(baseAssembly::get).collect(Collectors.toList());
        System.out.println(Arrays.toString(cleanup_order));
    }

    private void add_skeletons(String pathToNewsSkeletons, int pos) throws IOException {
        ArrayList<String> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(pathToNewsSkeletons))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(object -> files.add(object.toString()));
        }

        ArrayList<List<Joint>> skeletons = new ArrayList<>();

        for (String fl : files) {
            skeletons.add(get_skeleton(fl));
        }
        if (pos >= 0) {
            baseAssembly.addAll(pos, skeletons);
        } else {
            baseAssembly.addAll(skeletons);
        }

    }

    private List<Joint> get_skeleton(String fl) {
        List<Joint> skeleton = new ArrayList<>();

        BufferedReader reader = null;
        try {
            File file = new File(fl);
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                Double[] j = Arrays.stream(line.split(", ")).map(Double::parseDouble).toArray(Double[]::new);
                skeleton.add(new Joint(j[0], j[1], j[2], (int) Math.round(j[3])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert reader != null;
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return skeleton;

    }


    /**
     * Перестановка элементов базисной совокупности в заданном порядке
     *
     * @param order новый порядок,
     *              baseAssembly - базисная совокупность (gv)
     */
    private void change_ordering(Integer[] order) {
        this.baseAssembly =
                (ArrayList<List<Joint>>) Arrays.stream(order).map(baseAssembly::get).collect(Collectors.toList());
        System.out.println(Arrays.toString(order));
    }

    public double[][] getDistanceMatrix(String name, String f_path_video) {
        path_video = f_path_video;
        switch (name) {
            case "TST":
                readSkeletonCSVfromTST(path_video, skeletonVideoSequence);
                break;
            case "NTU":
                readSkeletonCSVfromNTU(path_video, skeletonVideoSequence);
                break;
        }
        if (skeletonVideoSequence.size() > 0) {
            return FDistance();
        } else
            return null;
    }

    public double[][] getStateMatrix() {
        double[][] S = new double[baseAssembly.size()][skeletonVideoSequence.size()];

        for (int i = 0; i < skeletonVideoSequence.size(); i++) {
            double state = 0.;
            for (int j = 0; j < 17; j++) {
                state += skeletonVideoSequence.get(i).get(j).getState();
            }
            state /= (17 * 2);
            for (int j = 0; j < baseAssembly.size(); j++) {
                S[j][i] = state;
            }
        }
        return S;
    }


    //Считывание csv файлов из базы данных TST
    private void readSkeletonCSVfromNTU(String pathDatabase, ArrayList<List<Joint>> skeletons) {
        File file = new File(pathDatabase);
        System.out.println(pathDatabase);
        skeletons.clear();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            int n_frames = Integer.parseInt(br.readLine());
            for (int i = 1; i < n_frames + 1; i++) {
                int number_humans_in_frame = Integer.parseInt(br.readLine());
                if (number_humans_in_frame > 1) {
                    skeletons.clear();
                    return;
                }
                br.readLine();
                int joints_number = Integer.parseInt(br.readLine());
                if (joints_number < 1) return;
                List<Joint> joints = new ArrayList<>();
                for (int j = 0; j < joints_number; j++) {
                    Double[] els =
                            Arrays.stream(br.readLine().split(" ")).map(Double::parseDouble).toArray(Double[]::new);
                    joints.add(new Joint(els[0] * 1000, els[1] * 1000, els[2] * 1000, (int) Math.round(els[els.length - 1])));
                }
                joints.remove(24);
                joints.remove(23);
                joints.remove(22);  //Removing unused joints
                joints.remove(21);
                joints.remove(19);
                joints.remove(15);
                joints.remove(11);
                joints.remove(7);
                skeletons.add(joints);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


//        for (SkeletonFrame skFrame : skeletonFrames) {
//            List<Joint> sk = skFrame.getPlayerCoordinates().get(0);
//            sk.remove(24);
//            sk.remove(23);
//            sk.remove(22);  //Removal unused joints
//            sk.remove(21);
//            sk.remove(19);
//            sk.remove(15);
//            sk.remove(11);
//            sk.remove(7);
//
//            skeletons.add(sk);
//
//        }

    }


    //Считывание csv файлов из базы данных TST
    private void readSkeletonCSVfromTST(String pathDatabase, ArrayList<List<Joint>> skeletons) {
        File file = new File(pathDatabase);
        SkeletonCSVParser parser = new SkeletonCSVParser();

        SkeletonObject skeletonObject = null;
        SkeletonFrame[] skeletonFrames = null;

        try {
            skeletonObject = parser.parse(file);
            skeletonFrames = skeletonObject.getSkeletonFrames();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (skeletonObject == null) {
            System.exit(0);
        }
        skeletons.clear();
        for (SkeletonFrame skFrame : skeletonFrames) {
            List<Joint> sk = skFrame.getPlayerCoordinates().get(0);
            sk.remove(24);
            sk.remove(23);
            sk.remove(22);  //Removing unused joints
            sk.remove(21);
            sk.remove(19);
            sk.remove(15);
            sk.remove(11);
            sk.remove(7);

            skeletons.add(sk);

        }

    }

    /**
     * Получить размер базисной совокупности
     *
     * @return
     */
    public int getAssemblySize() {
        return baseAssembly.size();
    }

    //Вычисление матрицы расстояний между двумя последовательностями скелетов
    private double[][] FDistance() {
        double[][] D = new double[baseAssembly.size()][skeletonVideoSequence.size()];
        /*       Processing skeleton data       */
        HeightCorrection(skeletonVideoSequence);
        AlingOnTheFloor(skeletonVideoSequence);
        SkeletOverlay(skeletonVideoSequence);

        HeightCorrection(baseAssembly);
        AlingOnTheFloor(baseAssembly);
        SkeletOverlay(baseAssembly);
        /*--------------------------------------*/
        // найти столбцы в матрице, которые будут заменены.
        //
        for (int i = 0; i < baseAssembly.size(); i++) {
            for (int j = 0; j < skeletonVideoSequence.size(); j++) {
                D[i][j] = EuclDists(baseAssembly.get(i), skeletonVideoSequence.get(j));
            }
        }
        // TODO Create a data with state information
        return D;
    }

    //Коррекция по высоте
    private void HeightCorrection(ArrayList<List<Joint>> sample) {
        //TODO temp thing
        double height;
        if (sample.size() == 32) {
            height = aver_height(sample);
        }
        else {
//            System.out.println(aver_height(sample));
            height = 1900;
        }
        for (List<Joint> skeleton : sample)
            for (Joint j : skeleton) {
                j.setX(j.getX() / height);
                j.setY(j.getY() / height);
                j.setZ(j.getZ() / height);
            }

    }

    //Смещение скелетов по высоте (ставим все скелеты на одну линию)
    private void AlingOnTheFloor(ArrayList<List<Joint>> sks) {
        //Выравнивание скелетов по полу
        double dY = 0.;
        for (List<Joint> sk : sks) {
            // поиск наименьшего значения Y
            dY = Integer.MAX_VALUE;
            for (Joint joint : sk) {
                if (dY > joint.getY()) {
                    dY = joint.getY();
                }
            }
            //Вычитание значения из всех Y (ставим на пол все скелеты)
            for (Joint joint : sk) {
                joint.setY(joint.getY() - dY);
            }
        }
    }

    //Совмещение скелетов по X и Y
    private void SkeletOverlay(ArrayList<List<Joint>> sks) {
        for (List<Joint> sk : sks) {
            for (int i = sk.size() - 1; i >= 0; i--) {
                //совмещение нулевых координат скелетов
                sk.get(i).setX(sk.get(i).getX() - sk.get(0).getX());
                //sk.get(i).setY(sk.get(i).getY() - sk.get(0).getY());
                sk.get(i).setZ(sk.get(i).getZ() - sk.get(0).getZ());
            }
        }

//        for (List<Joint> sk : sks) {
//            if (sk.get(16).getX() > sk.get(0).getX()) {
//                for (Joint joint : sk) {
//                    joint.setX(2 * sk.get(0).getX() - joint.getX());
//                }
//            }
//        }
    }

    //Евклидовое расстояние между двумя скелетами
    private double EuclDists(List<Joint> sk1, List<Joint> sk2) {
        double d = 0.;
        for (int i = 0; i < sk1.size(); i++) {
            d += Math.sqrt(
                    Math.pow(sk1.get(i).getX() - sk2.get(i).getX(), 2)
                            + Math.pow(sk1.get(i).getY() - sk2.get(i).getY(), 2)
                            + Math.pow(sk1.get(i).getZ() - sk2.get(i).getZ(), 2)
            );
        }

        d /= 17.;

        return d;
    }

    //Получение среднего роста по видеопоследовательности
    private double aver_height(ArrayList<List<Joint>> sampleData) {
        ArrayList<Double> dist = new ArrayList<>();

        for (int i = 0; i < 32; i++) {
            dist.add(person_height(sampleData.get(i)));
        }
        // Поиск первых десяти максимальных
        int border = 10;
        double[] res = new double[border];
        double out = 0.;
        for (int j = 0; j < border; j++) {
            res[j] = max(dist);
            out += res[j];
        }
        out /= 10.;

        return out;
    }

    //Вычисления роста человеку по скелетку при помощи геодезического расстояния
    private double person_height(List<Joint> skeletons) {
        double d1 = 0, d2 = 0;
        if (skeletons.size() > 17) {
            d1 += EuclDistance(skeletons.get(14), skeletons.get(13));
            d1 += EuclDistance(skeletons.get(13), skeletons.get(12));
            d1 += EuclDistance(skeletons.get(12), skeletons.get(0));
            d1 += EuclDistance(skeletons.get(0), skeletons.get(1));
            d1 += EuclDistance(skeletons.get(1), skeletons.get(20));
            d1 += EuclDistance(skeletons.get(20), skeletons.get(2));
            d1 += EuclDistance(skeletons.get(2), skeletons.get(3));

            d2 += EuclDistance(skeletons.get(18), skeletons.get(17));
            d2 += EuclDistance(skeletons.get(17), skeletons.get(16));
            d2 += EuclDistance(skeletons.get(16), skeletons.get(0));
            d2 += EuclDistance(skeletons.get(0), skeletons.get(1));
            d2 += EuclDistance(skeletons.get(1), skeletons.get(20));
            d2 += EuclDistance(skeletons.get(20), skeletons.get(2));
            d2 += EuclDistance(skeletons.get(2), skeletons.get(3));
        } else {
            d1 += EuclDistance(skeletons.get(12), skeletons.get(11));
            d1 += EuclDistance(skeletons.get(11), skeletons.get(10));
            d1 += EuclDistance(skeletons.get(10), skeletons.get(0));
            d1 += EuclDistance(skeletons.get(0), skeletons.get(1));
            d1 += EuclDistance(skeletons.get(1), skeletons.get(16));
            d1 += EuclDistance(skeletons.get(16), skeletons.get(2));
            d1 += EuclDistance(skeletons.get(2), skeletons.get(3));

            d2 += EuclDistance(skeletons.get(15), skeletons.get(14));
            d2 += EuclDistance(skeletons.get(14), skeletons.get(13));
            d2 += EuclDistance(skeletons.get(13), skeletons.get(0));
            d2 += EuclDistance(skeletons.get(0), skeletons.get(1));
            d2 += EuclDistance(skeletons.get(1), skeletons.get(16));
            d2 += EuclDistance(skeletons.get(16), skeletons.get(2));
            d2 += EuclDistance(skeletons.get(2), skeletons.get(3));
        }

        return (d1 + d2) / 2;
    }

    //Евклидовое расстояние
    private double EuclDistance(Joint a, Joint b) {
        return Math.sqrt(
                Math.pow(b.getX() - a.getX(), 2)
                        + Math.pow(b.getY() - a.getY(), 2)
                        + Math.pow(b.getZ() - a.getZ(), 2)
        );
    }

    //Поиск минимума в листе
    private double max(ArrayList<Double> d) {
        double max = Collections.max(d);
        d.remove(max);
        return max;
    }


}

