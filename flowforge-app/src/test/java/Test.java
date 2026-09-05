public abstract class Test {

    static  {
        System.out.println("Coming from Parent");
    }

    public static void main(String[] args) {
        Child child = new Child();
    }
}

class Child extends Test {


}
