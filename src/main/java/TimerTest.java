import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {

	private static Timer timer;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		timer = new Timer();
//		timer.schedule(new WorkTask(), 5000, 3000);
		timer.schedule(new WorkTask(), 1000);
	}
	
	
	public static class WorkTask extends TimerTask{
		
		@Override
		public void run()
		{
			System.out.println("Timer.");
			System.out.println("Timer..");
			System.out.println("Timer...");
			//timer.cancel();
			
		
		}
	}
}
