package com.mobimore;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {


    /**
     * @param args 0 - ширина окна; (0 если нужны целые видеоролики)
     *             1 - шаг смещения;
     *             2 - путь до базисной совокупности;
     *             3 - путь до базы данных ;
     *             4 - если нужна сортировка ("path to order"), если нет - ("");
     *             5 - Путь сохранения изображений активности
     */
    public static void main(String[] args) {
        //Введенные параметры
        int width_window = Integer.parseInt(args[0]);
        int shift_window = Integer.parseInt(args[1]);
        String basic_assembly_path = args[2];
        String database_name = args[3];
        String database_path = args[4];
        String order_for_assembly = args[5];
        String path_to_output = args[6];
        System.out.println(database_name);
        /*
          Создать объект класса для вычисления матрицы расстояний
          Отсортировать если требуется базисную совокупность.
          В итоге этот объект будет использоваться для получения
          матрицы расстояний видео с заданной базисной совокупностью
         */
        DistanceMatrix distanceMatrix = new DistanceMatrix(new String[]{basic_assembly_path, order_for_assembly});

        //Получить все пути к файлам из базы данных TST Fall Detection
        List<Path> filelist = getPaths(database_name, Paths.get(database_path));

        int processing = 0;
        for (Path rec : filelist) {
            processing++;
            if (processing % 1000 == 0)
                System.out.print(processing);
            if (rec.getFileName().toString().contains("._"))
                continue;
            //Получаем матрицу расстояний по файлу
            double[][] D = distanceMatrix.getDistanceMatrix(database_name, rec.toString());
            if (D == null) continue;
            double[][] S = distanceMatrix.getStateMatrix();
            int wind_length = width_window == 0 ? D[0].length : width_window;

            //Get info about fall
            int[] fallInfo = new int[]{0, 0};
            switch (database_name) {
                case ("NTU"):
                    if (rec.toString().contains("A043")) {
                        fallInfo = new int[] {1, D[0].length-2};
                    }
                    break;
                case ("TST"):
                    if (rec.toString().contains("Fall/")) {
                        try {
                            fallInfo = getFallInfo(rec.getParent().getParent().toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }


            //Нарезать окошки из матрицы
            int start_frame = 0;
            while (start_frame + wind_length <= D[0].length) {

                //Получить одно окно и выкинуть в файл
                SlidingWindow slidingWindow = sliceWindow(
                        new SlidingWindow(rec, wind_length, distanceMatrix.getAssemblySize(),
                                start_frame, fallInfo[0], fallInfo[1]), D);

                //Получить одно окно и выкинуть в файл
                SlidingWindow slidingWindow_forS = sliceWindow(
                        new SlidingWindow(rec, wind_length, distanceMatrix.getAssemblySize(),
                                start_frame, fallInfo[0], fallInfo[1]), S);

                if (!slidingWindow.dropToFile(path_to_output, database_name))
                    log("Файл не может быть записн");
//                if (!slidingWindow_forS.dropToFile(path_to_output + "_states", database_name))
//                    log("Файл не может быть записн");
                start_frame += shift_window;
            }
        }
    }

    /**
     * Считывание информации по падению
     */
    private static int[] getFallInfo(String path) throws IOException {
        BufferedReader br = null;

        br = new BufferedReader(new FileReader(path + "/fallMarks.csv"));

        String line = br.readLine();
        line = br.readLine();
        String[] info = line.split(",");
        int start_fall = Integer.parseInt(info[0]);
        int end_fall = Integer.parseInt(info[1].replace(" ", ""));

        return new int[]{start_fall, end_fall};
    }

    /**
     * Получить часть матрицы расстояний
     *
     * @param slidingWindow SlidingWindow object
     * @param D             Matrix of distances
     * @return
     */
    private static SlidingWindow sliceWindow(SlidingWindow slidingWindow, double[][] D) {
        double[][] window = new double[slidingWindow.getHeight()][slidingWindow.getWidth() != 0
                ? slidingWindow.getWidth() : D[0].length];

        //Вырезаем окно. Было бы здорово сделать через транспонирование и arraycopy
        //System.arraycopy(D, slidingWindow.getStartFrame(), window, 0, slidingWindow.getWidth());

        for (int i = 0; i < slidingWindow.getHeight(); i++) {
            if (slidingWindow.getWidth() >= 0)
                System.arraycopy(D[i],
                        slidingWindow.getStartFrame(),
                        window[i],
                        0,
                        slidingWindow.getWidth() == 0 ? D[0].length : slidingWindow.getWidth());
        }

        slidingWindow.setWindow(window);
        return slidingWindow;
    }

    private static List<Path> getPaths(String name, Path path) {
        List<Path> fl = null;
        String key;
        switch (name) {
            case ("TST"):
                key = "FileskeletonSkSpace.csv";
                try {
                    fl = Files.walk(path).
                            filter(Files::isRegularFile).
                            filter(e -> e.getFileName().toString().equalsIgnoreCase(key)).
                            collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case ("NTU"):
                key = ".skeleton";
                try {
                    fl = Files.walk(path).
                            filter(Files::isRegularFile).
                            filter(e -> e.getFileName().toString().contains(key)).
                            collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                return null;
        }


        return fl;
    }

    private static void log(String string) {
        System.out.println(string);
    }
}
