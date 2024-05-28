/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: PACKAGE_NAME
 * @Author: wyh
 * @Date: 2024-05-28 11:01
 * @Description: 测试主线程是否能够捕获到工作线程中抛出的异常
 */
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompleteTest {
    public static void main(String[] args) {
        try{
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            CompletableFuture<Void> cf = CompletableFuture.supplyAsync(()->{
                System.out.println(Thread.currentThread().getName());
                return null;
            }, executorService);
            cf.whenComplete((r, e)->complete());
            System.out.println(Thread.currentThread().getName() + " no error!");
        }catch(Exception e){
            System.out.println(Thread.currentThread().getName() + " error!");
        }
    }
    private static void complete(){
        System.out.println(Thread.currentThread().getName() + " error exist!!!");
        throw new RuntimeException("!!!!");
    }
}
