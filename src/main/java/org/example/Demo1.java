package org.example;

public class Demo1 {
    public static void main(String[] args) {
        Animal myDog = new Dog();
        myDog.speak();

        Animal myCat = new Cat();
        myCat.speak();

        Flying myBird = new Bird();
        myBird.fly();
    }
}

interface Flying {
    void fly();
}

abstract class Animal {
    abstract void speak();
}

class Dog extends Animal {
    @Override
    void speak() {
        System.out.println("Woof Woof!");
    }
}

class Cat extends Animal {
    @Override
    void speak() {
        System.out.println("Meow!");
    }
}

class Bird extends Animal implements Flying {
    @Override
    void speak() {
        System.out.println("Tweet!");
    }

    @Override
    public void fly() {
        System.out.println("Flaps wings and flies!");
    }
}