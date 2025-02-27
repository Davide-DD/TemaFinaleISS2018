package fourthTest;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import alice.tuprolog.SolveInfo;
import it.unibo.qactors.akka.QActor;

public class FourthTest {

	private QActor robot = null, console = null, environment = null, sensor = null, businessLogic = null, led = null;

	protected void runContextsAndGetQActors() { 
		CtxRun consoleRun = new CtxRun("consolectx4", 
				"./srcMore/it/unibo/consoleCtx4/cleaningrobotanalisirequisiti4.pl", 
				"./srcMore/it/unibo/consoleCtx4/sysRules.pl", new String[]{"qaconsole4_ctrl"});
		CtxRun robotRun = new CtxRun("robotctx4", 
				"./srcMore/it/unibo/robotCtx4/cleaningrobotanalisirequisiti4.pl", 
				"./srcMore/it/unibo/robotCtx4/sysRules.pl", 
				new String[]{"qarobot4_ctrl", "qaenvironment_ctrl", 
						"qaproximitysensor_ctrl", "qabusinesslogic_ctrl", "qaled_ctrl"});
		
		Thread consoleThread = new Thread(consoleRun);
		consoleThread.start();
		
		Thread robotThread = new Thread(robotRun);
		robotThread.start();
		
		try {
			consoleThread.join();
			robotThread.join();
			
			console = consoleRun.getQActors().iterator().next();
			for (QActor q : robotRun.getQActors()) {
				if (q.getName().equals("qarobot4_ctrl")) {
					robot = q;
				}
				else if (q.getName().equals("qaenvironment_ctrl")) {
					environment = q;
				}
				else if (q.getName().equals("qaproximitysensor_ctrl")) {
					sensor = q;
				}
				else if (q.getName().equals("qabusinesslogic_ctrl")) {
					businessLogic = q;
				}
				else if (q.getName().equals("qaled_ctrl")) {
					led = q;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	@Before
	public void systemSetUp() throws Exception	{
		System.out.println("systemSetUp starts " );
		runContextsAndGetQActors();
	}

	@After
	public void terminate()	{
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("====== terminate " ); 
	}
	
	/**
	 *  R-Start: il robot deve iniziare la pulizia della stanza quando viene premuto start.
	 *  
	 *  R-BlinkLed: appena ricevuto il comando di start, per come e' strutturato il sistema, basta vedere se lo stato del led diventa lampeggiante
	 *  
	 *  Le precondizioni di questi eventi coincidono, quindi verranno trattati tutti qui sotto
	 */  
	@Test
	public void rStart() {
		System.out.println("====== rStart ===============" ); 
		try {
			console.sendMsg("consoleMsg", robot.getNameNoCtrl(), "dispatch", "consoleMsg(start)");
			
			Thread.sleep(1000);
			
			SolveInfo sol = robot.solveGoal("rStart");
			assertTrue("", sol.isSuccess());
			
			sol = led.solveGoal("rBlinkLed");
			assertTrue("", sol.isSuccess());
			
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
	/**
	 * 	R-Stop: come per lo start, lo stop e' permesso solo quando il robot sta effettivamente pulendo.
	 *  Di conseguenza, basta semplicemente verificare che lo stato del sistema (che e' stato gia' verificato attivarsi con successo con R-Start),
	 *  cambi in stoppato.
	 *  I led pero' continueranno a lampeggiare: bisogna quindi controllare che anch'essi si spengano
	 */
	@Test
	public void rStop() {
		System.out.println("====== rStop ===============" ); 
		try {
			console.sendMsg("consoleMsg", robot.getNameNoCtrl(), "dispatch", "consoleMsg(stop)");
			
			Thread.sleep(1000);

			SolveInfo sol = robot.solveGoal("rStop");
			assertTrue("", sol.isSuccess());
			
			sol = led.solveGoal("rStop");
			assertTrue("", sol.isSuccess());
			
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
	/**
	 * 	R-TempKo: vale lo stesso discorso fatto per R-TempOk.
	 *  Di conseguenza, basta verificare che lo stato del sistema si disattiva quando la condizione e' violata
	 *  I led potrebbero essere lampeggianti: bisogna quindi controllare che si spengano in questo caso
	 */
	@Test
	public void rTempKo() {
		System.out.println("====== rTempKo ===============" ); 
		try {
			environment.emit("tempEvent", "tempEvent(44)");

			Thread.sleep(250);

			SolveInfo sol = businessLogic.solveGoal("rTempKo");
			assertTrue("", sol.isSuccess());
			
			sol = led.solveGoal("rStop");
			assertTrue("", sol.isSuccess());
					
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
	/**
	 * R-TimeKo: come sopra
	 */
	@Test
	public void rTimeKo() {
		System.out.println("====== rTimeKo ===============" ); 
		try {
			environment.emit("timeEvent", "timeEvent(5)");

			Thread.sleep(250);

			SolveInfo sol = businessLogic.solveGoal("rTimeKo");
			assertTrue("", sol.isSuccess());
			
			sol = led.solveGoal("rStop");
			assertTrue("", sol.isSuccess());
			
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
	/**
	 * 	R-TempOk: il robot puo' ricevere il comando di start se e solo se la temperatura (e il tempo, indicato sotto) e' ok.
	 *  Dato che questo e' vincolato dal frontend, e' necessario semplicemente verificare se 
	 *  quando la temperatura e' ok (e anche il tempo lo e'), lo stato del sistema torna attivo.
	 *  
	 *  R-TimeOk: come sopra ma per il tempo
	 *  
	 *  Dato che devono essere entrambi veri per l'attivazione del sistema, verranno trattati qui sotto insieme
	 */
	@Test
	public void rTempAndTimeOk() {
		System.out.println("====== rTempAndTimeOk ===============" ); 
		try {
			environment.emit("timeEvent", "timeEvent(10)");
			environment.emit("tempEvent", "tempEvent(20)");
			
			Thread.sleep(1000);
			
			SolveInfo sol = businessLogic.solveGoal("rTempAndTimeOk");
			assertTrue("", sol.isSuccess());
			
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
	/**
	 * REQUISITO AGGIUNTIVO
	 * 
	 * R-RobotObstacle: controlla che il robot riceva l'evento di collisione emesso dal sensore
	 */
	@Test
	public void rRobotObstacle() {
		System.out.println("====== rRobotObstacle ===============" ); 
		try {
			sensor.emit("collisionEvent", "collisionEvent(test)");

			Thread.sleep(1000);

			SolveInfo sol = robot.solveGoal("rRobotObstacle");
			assertTrue("", sol.isSuccess());
			
		} catch (Exception e) {
			System.out.println( "ERROR=" + e.getMessage() ); fail("actorTest " + e.getMessage() );
		}
	}
	
}
