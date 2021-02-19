package utils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileUtils {
    FileWriter fw;

    public FileUtils(String path, String filename) {
        File directory = new File(path);
        if (! directory.exists())
            directory.mkdirs();
        try {
            this.fw = new FileWriter( path + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String data){
        try {
            fw.write(data +"\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileWriter openFile(String path, String filename) throws IOException {
        File directory = new File(path);
        if (! directory.exists())
            directory.mkdirs();
        return new FileWriter(filename);
    }

    public static Object loadFile(File file) throws IOException, ClassNotFoundException {
        FileInputStream streamIn = new FileInputStream(file);
        ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
        Object obj = objectinputstream.readObject();
        objectinputstream.close();
        return obj;
    }

    public static File selectFile(String folder) throws FileNotFoundException {
        File folders = new File(folder);

        System.out.println("Select folder: \n");
        File[] files = folders.listFiles();
        if (files == null || files.length == 0)
            throw new FileNotFoundException();
        Arrays.sort(files, Comparator.comparing(File::getAbsolutePath));

        for(int i = 0; i < files.length; i ++)
            System.out.println(i + " -> " + files[i].getName());

        int selected = -1;
        while (selected < 0 || selected >= files.length) {
            System.out.print("Select a folder id >> ");
            selected = new Scanner(System.in).nextInt();
        }

        return files[selected];
    }

    public static void writeStringToFile(String filename, String data) throws IOException {
        FileWriter myWriter = new FileWriter(filename);
        myWriter.write(data);
        myWriter.flush();
        myWriter.close();
    }

    public static void writeObjectToFie(String fileName, Serializable obj) throws IOException {
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fout = new FileOutputStream( fileName);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(obj);
        }finally {
            if (oos != null)
                oos.close();
        }
    }

    public static void createFolder(String folderPath){
        File directory = new File(folderPath);
        if (! directory.exists())
            directory.mkdirs();
    }

    public static List<String> listFilesNamesFromFolder(String folderName) throws FileNotFoundException {
        File folder = new File(folderName);
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            throw new FileNotFoundException();
        }
        return Arrays.stream(files).map( File::getName).collect(Collectors.toList());
    }
}
