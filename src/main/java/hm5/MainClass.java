package hm5;
//Организуем гонки:
//1.Все участники должны стартовать одновременно, несмотря на то что
// на подготовку у каждого их них уходит разное время *CyclicBarrier*
//2.В туннель не может заехать одновременно больше половины участников
// (условность) Попробуйте всё это синхронизировать. *semaphore*
// 3.Только после того как все завершат гонку нужно выдать объявление
// об окончании*CountDownLatch* или *CyclicBarrier*
//Можете корректировать классы (в т.ч. конструктор машин) и добавлять
// объекты классов из пакета util.concurrent
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class MainClass {
    public static final int CARS_COUNT = 4;
    static CountDownLatch start = new CountDownLatch(CARS_COUNT);
    static Semaphore tunnel = new Semaphore(CARS_COUNT/2, true);
    static CountDownLatch finish = new CountDownLatch (CARS_COUNT);
    static CyclicBarrier stage = new CyclicBarrier(CARS_COUNT);

    public static void main(String[] args) {

        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Подготовка!!!");
        Race race = new Race(new Road(60), new Tunnel(), new Road(40));
        Car[] cars = new Car[CARS_COUNT];
        for (int i = 0; i < cars.length; i++) {
            cars[i] = new Car(race, 20 + (int) (Math.random() * 10));
        }
        for (int i = 0; i < cars.length; i++) {
            new Thread(cars[i]).start();
        }
        while (start.getCount() > 0) {   //собираемся
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка началась!!!");

        while (finish.getCount() > 0) {       // проверяем все ли проехали
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ВАЖНОЕ ОБЪЯВЛЕНИЕ >>> Гонка закончилась!!!");
    }
}

class Car implements Runnable {
   private static int CARS_COUNT = 0;
   private final Race race;
   private final int speed;
   private final String name;

   public String getName() {  return name; }
   public int getSpeed() { return speed; }

   public Car(Race race, int speed) {
       this.race = race;
       this.speed = speed;
       CARS_COUNT++;
       this.name = "Участник #" + CARS_COUNT;
   }
   @Override
   public void run() {
       try {
           System.out.println(this.name + " готовится");
           Thread.sleep(500 + (int)(Math.random() * 800));
           MainClass.start.countDown();
           MainClass.start.await();
           System.out.println(this.name + " готов");
       } catch (Exception e) {
           e.printStackTrace();
       }
       for (int i = 0; i < race.getStages().size(); i++) {
           race.getStages().get(i).go(this);
       }
   }
}

abstract class Stage {
    protected int length;
    protected String description;

    public String getDescription() {
        return description;
    }
    public abstract void go(Car c);

}

class Road extends Stage {
        public Road(int length) {
        this.length = length;
        this.description = "Дорога " + length + " метров";
    }
    @Override
    public void go(Car c) {
        try {
            System.out.println(c.getName() + " начал этап: " + description);
            Thread.sleep(length / c.getSpeed() * 1000L);
            MainClass.stage.await();
            System.out.println(c.getName() + " закончил этап: " + description);
            if (this.length == 40) {
            MainClass.finish.countDown();}
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
    }

class Tunnel extends Stage {
    public Tunnel() {
        this.length = 80;
        this.description = "Тоннель " + length + " метров";
    }
    @Override
    public void go(Car c) {
        try {
            try {
                System.out.println(c.getName() + " готовится к этапу(ждет): " + description);
                MainClass.tunnel.acquire();
                System.out.println(c.getName() + " начал этап: " + description);
                Thread.sleep(length / c.getSpeed() * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                MainClass.tunnel.release();
                System.out.println(c.getName() + " закончил этап: " + description);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Race {
    private final ArrayList<Stage> stages;

    public ArrayList<Stage> getStages() { return stages; }
    public Race(Stage... stages) {
      this.stages = new ArrayList<>(Arrays.asList(stages));
          }
}


