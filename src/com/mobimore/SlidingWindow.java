package com.mobimore;

import java.io.*;
import java.nio.file.Path;
import java.util.Locale;

public class SlidingWindow {

    private int window_width;
    private int window_height;
    private int startFrame;

    private boolean binary_FALL = false;
    private double float_FALL;
    private boolean inWindow = false;
    private boolean full_video_size = false;

    private double[][] window;

    private Path video_path;
    private String window_pathname;
    private String window_name;

    //Будет использоваться для хранения полной инфомрации о получаемом изображении

    public SlidingWindow(Path rec, int window_width,
                         int height_window,
                         int start_frame,
                         int start_fall,
                         int end_fall) {
        this.video_path = rec;
        this.window_width = window_width;
        this.full_video_size = window_width == 0;
        this.window_height = height_window;
        this.startFrame = start_frame;


        //Определение падения на отрезке и вычисление отношения продолжительности падения в окне, к
        if ((end_fall > 0 && start_fall >= 0)
                && (start_fall <= getEndFrame() && (end_fall >= start_frame))) {
            {
                this.binary_FALL = true;
                int start_local_fall = Math.max(start_fall, start_frame);
                int end_local_fall = Math.min(end_fall, getEndFrame());
                int duration = end_local_fall - start_local_fall + 1;
                this.float_FALL = (double) duration / window_width;
                if ((duration == window_width) || ((duration < window_width)
                        && ((start_fall >= start_frame) && (end_fall <= getEndFrame())))) {
                    inWindow = true;
                }
            }
        }

    }


    /**
     * Получить ширину окна
     */
    public int getWidth() {
        return window_width;
    }

    /**
     * Получить высоту окна
     */
    public int getHeight() {
        return window_height;
    }

    /**
     * Get frame number, which slice window is beginning
     */
    public int getStartFrame() {
        return this.startFrame;
    }

    /**
     * Get number of end frame. End of window in the video
     */
    public int getEndFrame() {
        return this.startFrame + this.getWidth() - 1;
    }

    public void setWindow(double[][] window) {
        this.window = window;
    }

    public boolean dropToFile(String path_to_output, String database_name) {

        //Создадим путь для окна
        // Версия: не записывать ADL, в которых есть часть падения (next string)
//        if (!this.inWindow && this.binary_FALL) {
//            return true;
//        }
//        else {
        switch (database_name) {
            case ("TST"):
                this.window_pathname = generatePathforTST(path_to_output);
                break;
            case ("NTU"):
                this.window_pathname = generatePathforNTU(path_to_output);
                break;
        }
        File folder = new File(this.window_pathname);
        folder = folder.getParentFile();
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                log("Создана новая директория: " + folder);
            } else {
                log("Директория не может быть создана");
                return false;
            }
        }
        try (FileWriter writer = new FileWriter(this.window_pathname, false)) {
            for (double[] doubles : window) {
                for (int j = 0; j < doubles.length; j++) {
                    if (j < doubles.length - 1)
                        writer.append(String.format("%.6f ", doubles[j]));
                    else
                        writer.append(String.format("%.6f", doubles[j]));
                }
                writer.append("\n");
            }
            writer.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }


//        }

        return true;
    }

    private String generatePathforTST(String path_to_output) {
        String[] strs = new String[4];
        File temp = new File(video_path.getParent().toString());
        for (int i = 0; i < 4; i++) {
            temp = new File(temp.getParent());
            strs[i] = temp.getName();
        }
        String startFall = null;
        String endFall = null;

        String everything = "";
        if (strs[2].equals("Fall")) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(video_path.getParent().getParent() + "/fallMarks.csv"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    line = br.readLine();
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
                everything = sb.toString();
                startFall = everything.split(", ")[0];
                endFall = everything.split(", ")[1];
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }


        String fp = String.format("%03d", Integer.parseInt(strs[3].substring(4)));
        String string = "TST" + fp
                + "/" + strs[3].substring(0, 4)
                + fp
                + strs[1].toUpperCase() + strs[0]
                + String.format("_%04d_", this.getStartFrame()) + String.format("%04d_", this.getEndFrame())
                + (binary_FALL ? ("T_") : ("F_")) + (this.inWindow ? ("T_") : ("F_"))
                + String.format(Locale.ROOT, "%.5f", float_FALL) + "_fallFrame_"
                + (startFall != null ? (startFall + "_") : ("0_")) + (endFall != null ? (endFall) : ("0")) + ".csv";
        string = path_to_output + string;
        return string;
    }

    private String generatePathforNTU(String path_to_output) {
        String string = video_path.getFileName().toString().split("\\.")[0]
                + String.format("_%04d_", this.getStartFrame()) + String.format("%04d_", this.getEndFrame())
                + (binary_FALL ? ("T_") : ("F_")) + (this.inWindow ? ("T_") : ("F_"))
                + String.format(Locale.ROOT, "%.5f", float_FALL) + ".csv";
        string = path_to_output + string;
        return string;
    }

    private static void log(String string) {
        System.out.println(string);
    }
}
