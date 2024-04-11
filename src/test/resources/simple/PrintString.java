public class PrintString {
    public static void printStringArray(String[] strings) {
        for (String str : strings) {
            System.out.print(str + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        String[] strings = {"Java", "is", "awesome"};
        System.out.println("Array of strings:");
        printStringArray(strings);
    }
}