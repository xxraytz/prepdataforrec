package com.mobimore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MatrixParameters {

    private static final int DIM_MATRIX = 136;

    public static void index_correction(int[] order) {

        boolean check = false;

        for (int i = 0; i < DIM_MATRIX; i++) {
            if (order[i] == 0)
                return;
        }

        for (int i = 0; i < DIM_MATRIX; i++) {
            order[i] -= 1;
        }
    }

    public static double get_min_edge(double[][] matrix) {

        double res;

        int mini = 1;
        for (int i = 1; i < DIM_MATRIX - 1; i++) {
            if (matrix[mini - 1][mini] > matrix[i][i + 1])
                mini = i + 1;
        }

        res = matrix[mini - 1][mini];
        return res;
    }

    public static double get_max_edge(double[][] matrix) {
        double res;

        int maxi = 1;

        for (int i = 1; i < DIM_MATRIX - 1; i++) {
            if (matrix[maxi - 1][maxi] <  matrix[i][i + 1])
                maxi = i + 1;
        }

        res = matrix[maxi - 1][maxi];
        return res;
    }

    public static double get_median_dpath(double[][] matrix) {
        double res;

        res = matrix[(DIM_MATRIX - 1) / 2][(DIM_MATRIX - 1) / 2 + 1];

        return res;
    }

    public static double get_average_between_nodes_dpath(double[][] matrix) {

        double res = 0.;

        res = get_length_upper_main_diag(matrix) / (DIM_MATRIX - 1);

        return res;
    }

    public static double get_distance_bw_teminate_nodes(double[][] matrix) {
        double res = 0.;
        res = matrix[0][DIM_MATRIX - 1];
        return res;
    }

    public static double get_length_upper_main_diag(double[][] matrix) {

        double res = 0.;

        for (int i = 0; i < DIM_MATRIX-1; i++) {
            res += matrix[i][i+1];
        }

        return res;
    }

    public static double[][] merge(double[][] matrix, int[] order) {

        double[][] res = new double[DIM_MATRIX] [DIM_MATRIX];

        for (int i = 0; i < DIM_MATRIX; i++) {
            for (int j = 0; j < DIM_MATRIX; j++) {
                res[i][j] = matrix[order[i]][order[j]];
            }
        }

        return res;
    }


    public static Integer[] get_order(String path_order) throws IOException {

        BufferedReader br = null;

        br = new BufferedReader(new FileReader(path_order));

        String line;

        String tstr = "";

        tstr = br.readLine();
        String info = br.readLine();
        log("Импортирован новый порядок базисной совокупности. " + info + "\n");
        String[] strings;

        Integer[] order = new Integer[DIM_MATRIX];

        strings = tstr.split(" ");

        for (int i = 0; i < DIM_MATRIX; i++) {
            order[i] = Integer.parseInt(strings[i]);
        }

        return order;
    }

    private static void log(String string){
        System.out.println(string);
    }

/*

    public static double[][] getMatrix() throws IOException {
        //Read files
        BufferedReader br = null;

        br = new BufferedReader(new FileReader(new File(PATH_MATRIX)));

        String line;

        double[][] matrix = new double[DIM_MATRIX][DIM_MATRIX];

        ArrayList<String> tempArr = new ArrayList<>();

        while ((line = br.readLine()) != null)
        {
            if (!line.isEmpty())
            {
                tempArr.add(line);
            }
        }

        String[][] tempstr = new String[DIM_MATRIX][DIM_MATRIX];

        for (int i = 0; i < DIM_MATRIX; i++) {
            tempstr[i] = tempArr.get(i).split(",");
        }

        for (int i = 0; i < DIM_MATRIX; i++)
        {
            for (int j = 0; j < DIM_MATRIX; j++) {
                matrix[i][j] = Double.parseDouble(tempstr[i][j]);
            }
        }

        return matrix;
    }

*/
}
