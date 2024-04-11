public class PrintInt {
    public static void printIntArray(int x) {
        for (int i = 0;i< x ; i++) {
            if(x % 2 == 0) {
                System.out.println(x + " is Even");
            }else {
                System.out.println(x + " is Odd");
            }
        }
    }

    public static void main(String[] args) {
        int nums = 5;
        System.out.println("Array of integers:");
        printIntArray(nums);
    }
}