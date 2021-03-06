
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;//arrayList for all printed objects
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;//scan files beatmap files
import java.util.LinkedList;
import java.util.concurrent.*;//for cyclic barrier
import java.io.*;
import javax.sound.sampled.*;

public class GameBoard extends JPanel implements ActionListener,MouseMotionListener{
    protected double systemTime;//update every action performed, remove timer1
    protected Timer timer;
    protected double time,currentTime,startTime,lastHitTime;
    //scans notes, etc.
    protected String dir;
    protected Scanner mapInput;
    protected String nextLine;
    //game mechanics vars
    protected boolean mouseDown,mouseClicked,key1down,key2down;
    protected double error;//the offset of clicking a note
    protected int combo,score;
    protected int circleSize;//the size of each note.
    //----create boolean to prevent repeated inputs with 1 input
    protected boolean key1used,key2used;
    protected int mouseX, mouseY;
    //printable objects
    protected Background bgPanel;
    protected Image approachCircle;
    protected Image hitCircle;
    protected Image ball;
    protected SliderBall sliderBall;
    protected LinkedList<Integer> criticalPts;

    protected Image hit300,hit300g,hit300k,hit100,hit100k,hit50,hit0;
	
    protected List<PrintableObject> objects;
    protected List<HitNum> hitNums;
    //unite all circles and sliders into 1 list<object>
    protected Window w;
    protected String songPath;
    protected MP3 song;
    protected String videoPath;
    protected Playback v;
    //might replace playback with a videoworker that accepts the dir.
    protected VideoWorker vw;
    protected Thread t1;
    protected boolean video;
    protected boolean closed;
    public GameBoard(String dir,String filename,int circleSize,boolean video){
	//set up component, might need more
	nextLine="";
     	setFocusable(true);
	setVisible(true);
	this.dir=dir;
	this.video=video;
	setOpaque(false);
	closed=false;
	
	setLayout(new BorderLayout());
	addKeyListener(new TAdapter());
	addMouseListener(new MAdapter());
	addMouseMotionListener(this);
	File currentSongDir = new File(dir);
	if (currentSongDir!=null){
	    File[] files = currentSongDir.listFiles();
	    for (File f:files){
		if (f.getPath().contains("ackground")){
		    bgPanel= new Background(f.getPath());
		}
	    }
	}

	//open the scanned file
	try{
	    //start up scanner
            mapInput = new Scanner(new File(dir + filename));
	    while (mapInput.hasNext()){
		nextLine = mapInput.nextLine();
		if (!nextLine.substring(0,1).equals("#")){
		    break;
		}
	    }
	}catch(FileNotFoundException exception){
	    exception.printStackTrace();
	}
	
	//open the stupid multimedia files here//
	//test.txt must follow the format, otherwise it crashes on you
	if (nextLine.equals("song")){
	    songPath=mapInput.nextLine();
	}else{
	    throw new RuntimeException();
	}
	song = new MP3(dir+songPath);
	//load all images into memory	
	approachCircle = new ImageIcon("default/approachCircle.png").getImage();
	hitCircle = new ImageIcon("default/hitcircle.png").getImage();
	hit300 = new ImageIcon("default/hit300.png").getImage();
	hit300g= new ImageIcon("defaulthit300g.png").getImage();
	hit300k= new ImageIcon("default/hit300k.png").getImage();
	hit100= new ImageIcon("default/hit100.png").getImage();
	hit100k= new ImageIcon("default/hit100k.png").getImage();
	hit50= new ImageIcon("default/hit50.png").getImage();
	hit0= new ImageIcon("default/hit0.png").getImage();
	ball = new ImageIcon("default/sliderb0.png").getImage();
	sliderBall = new SliderBall();

	lastHitTime=0;
	this.circleSize = circleSize;
	objects=new ArrayList<PrintableObject>();
	hitNums = new ArrayList<HitNum>();
	criticalPts = new LinkedList<Integer>();
        nextLine = mapInput.nextLine();
	if (nextLine.equals("video")){
	    videoPath=mapInput.nextLine();
	    nextLine=mapInput.next();
	    // ^^^^to align both cases of having both video and no video
	}
	System.out.println(nextLine);
	//else{
	//    there is no video. assume the next line is the beginning
	//    of a new note.
	
	//here begins the test.txt file reading.
	while (mapInput.hasNext()){
	    if (nextLine.equals("C")){
		int x=(int)(mapInput.nextInt()*1.5);
		int y=(int)(mapInput.nextInt()*1.5);
		double time=mapInput.nextInt()/1000.0;
		int orderNum=0;//removed functionality
		objects.add(new Circle(x,y,orderNum,time));
		//System.out.println("after 1 obj" + nextLine +"!!!");
		if (mapInput.hasNext())
		    nextLine=mapInput.nextLine();
		else
		    break;//we dont want this crashing our scanner
	    }else if (nextLine.equals("S")){
		criticalPts=new LinkedList<Integer>();//refresh list
		int x=(int)(mapInput.nextInt()*1.5);
		int y=(int)(mapInput.nextInt()*1.5);
		double time=mapInput.nextInt()/1000.0;
		int orderNum=0;
		//numOfSteps: 0=linear, 1=quad curve, 2 = cubic bezier, so on.
		//basically the number of controlpoints
		ArrayList<Point> points = new ArrayList<Point>();
		mapInput.nextInt();
		mapInput.nextInt();
		// ^^^this is useless because multiple versions of osu files are used
		//queue usage here.
		mapInput.next();
		String[] theRest = mapInput.nextLine().split(" ");
		for (String s:theRest){
		    if (s.length()!=0)
			criticalPts.addLast(new Integer((int)(((double)Double.valueOf(s))*1.5)));
		}
		int numOfSteps = mapInput.nextInt();
		points.add(new Point(x,y));//start point for slider
		for (int index=0;index<=numOfSteps;index++){
		    Integer i=criticalPts.removeFirst();
		    Integer j=criticalPts.removeFirst();
		    points.add(new Point(i,j));
		}
		Slider s =new Slider(x,y,orderNum,time,points,numOfSteps);
		s.findPoints();
		//System.out.println(points.size());
		objects.add(s);
		if (mapInput.hasNext())
		    nextLine=mapInput.nextLine();
		else
		    break;
	    }else{
		nextLine=mapInput.nextLine();
	    }
	}
	System.out.println("Size"+objects.size());
	timer = new Timer(20,this);
	timer.start();
	//requestFocusInWindow();
	//set cursor w/ toolkit here
    }
    public void streamMedia(){
	t1 = new Thread(song);
	if (videoPath!=null){
	    System.out.println("This has a video.");
	    System.out.println(dir+videoPath);
	    vw = new VideoWorker(this,dir,videoPath);
	    if (video){
		add(vw.p);//readd this after testing
		vw.execute();
		w.repaint();
		w.revalidate();
	    }
	}
	t1.start();
	startTime = System.currentTimeMillis()/1000.0;
    }
    public void setFrame(Window w){
	this.w = w;
    }
    public Window getFrame(){
	return w;
    }
    public void paint(Graphics g){
	super.paint(g);
	Graphics2D g2d = (Graphics2D)g;
	if (!video || videoPath==null){
	    bgPanel.paint(g2d);
	    System.out.println("lol no video");
	}
	if (startTime > 0){
	    for (PrintableObject o:objects){
		
		
		if (o instanceof Circle){
		    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,o.transparency));
		    g2d.drawImage(hitCircle,o.x-circleSize/2,
				  o.y-circleSize/2,
				  circleSize,circleSize,this);
		}else if (o instanceof Slider){
		    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, o.transparency));
		    for (Point p:((Slider)o).pathOfPoints){
			//just using constants for the sake of time
			g2d.drawImage(hitCircle,(int)p.getX()-35,(int)p.getY()-35,70,70,this);
		    }
		    g2d.drawImage(hitCircle,(int)((Slider)o).points.get(0).getX()-35,
				  (int)((Slider)o).points.get(0).getY()-35,70,70,this);
		}
		if (startTime+o.time>currentTime+5){
		    break;
		}
		g2d.drawImage(approachCircle,o.x-o.aCircleSize/2,
			    o.y-o.aCircleSize/2,
			    o.aCircleSize,o.aCircleSize,this);
		//also draw for sliders lol
	    }
	    for (HitNum h:hitNums){
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,h.transparency));
		if (h.score==300){
		    g2d.drawImage(hit300,h.x-25,h.y-15,70,45,this);
		}else if (h.score==100){
		    g2d.drawImage(hit100,h.x-25,h.y-15,70,45,this);
		}else if (h.score==50){
		    g2d.drawImage(hit50,h.x-25,h.y-15,70,45,this);
		}else if (h.score==0){
		    g2d.drawImage(hit0,h.x-25,h.y-15,50,45,this);
		}
	    }
	}
	Toolkit.getDefaultToolkit().sync();
	g.dispose();
    }
    public boolean hitNote(PrintableObject o){
	error = Math.abs(o.aCircleSize-70)/70.0;
	if (o.aCircleSize<150){
	    if (Math.pow(mouseX-o.x,2)+Math.pow(mouseY-o.y,2) < 1225 && error < .15){
		objects.remove(o);
		hitNums.add(new HitNum(o.x,o.y,300));
		playHit();
		return true;
	    }else if (Math.pow(mouseX-o.x,2)+Math.pow(mouseY-o.y,2) < 1600){
		if (error < .4){
		    objects.remove(o);
		    hitNums.add(new HitNum(o.x,o.y,100));
		    playHit();
		    return true;
		}else if (error < .8){
		    objects.remove(o);
		    hitNums.add(new HitNum(o.x,o.y,50));
		    playHit();
		    return true;
		}
	    }
	}
	return false;
    }
    public void playHit(){
	try{
	    Clip hitnormal;
	    hitnormal= AudioSystem.getClip();
	    hitnormal.open(AudioSystem.getAudioInputStream(new File("default/normal-hitnormal.wav")));
	    hitnormal.start();
	}catch(Exception e){
	    e.printStackTrace();
	    
	}
    }
    public void actionPerformed(ActionEvent e){
	currentTime = System.currentTimeMillis()/1000.0;
	time+=0.02;
	//under click->true,set currentTime. if start+o.time>current+5 break
	//compare the x,y of the FIRST OBJECT, as well as timing.
	////////////////////////YOU WERE LAST HERE ERIC////////
	if (mouseClicked){
	    mouseClicked=false;
	    //check first element objects here
	    if (objects.size() > 0){
		//		PrintableObject o = objects.get(0);
	        for (int i=0;i<objects.size();i++){
		    PrintableObject o = objects.get(i);
		    if (startTime+o.time>currentTime+5){
			break;//ignore the beats too far ahead in the future.
		    }else{
			boolean b = hitNote(o);
			if (b)
			    break;//only 1 click per note!!
		    }
		    
		}
	    }
	}/////////////check key1
	if (key1down && key1used==false){
	    key1used=true;
	    //check first element here
	    if (objects.size() > 0){
	        //PrintableObject o = objects.get(0);
	        for (int i=0;i<objects.size();i++){
		    PrintableObject o = objects.get(i);
		    if (startTime+o.time>currentTime+5){
			break;//ignore the beats too far ahead in the future.
		    }else{
			boolean b = hitNote(o);
			if (b)
			    break;//only 1 click per note!!
		    }
		    
		}
	    }
	}else if(key1used && !key1down){
	    key1used=false;
	}//////////////check key2
	if (key2down && key2used==false){
	    key2used=true;
	    //check first element here
	    if (objects.size() > 0){
	        //PrintableObject o = objects.get(0);
	        for (int i=0;i<objects.size();i++){
		    PrintableObject o = objects.get(i);
		    if (startTime+o.time>currentTime+5){
			break;//ignore the beats too far ahead in the future.
		    }else{
			boolean b = hitNote(o);
			if (b)
			    break;//only 1 click per note!!
		    }
		    
		}
	    }
	}else if(key2used && !key2down){
	    key2used=false;
	}
	
	//do game actions here
	if (startTime>0){//if the game has commenced
	    for (int i=0;i<objects.size();i++){
		PrintableObject o = objects.get(i);  
		if (o.time+startTime<1+currentTime){
		    if (o.transparency<0.9)
			o.transparency+=0.10;
		    //x = k-kt x =k @ t=0 and x=0 @ t=1
		    //or x = k(c-t) where k is the shrink constant
		    //and c is the time of shrinking.
		    o.aCircleSize=(int)(150*(startTime+o.time-currentTime)+circleSize);
		}
		if(currentTime - 0.5 > startTime+o.time){
		    //.25 seconds after you MISSED it...
		    objects.remove(o);
		    System.out.println("oops");
		    //insert X image lol
		    hitNums.add(new HitNum(o.x,o.y,0));
		}
		if (startTime+o.time>currentTime+5){
		    break;//ignore the beats too far ahead in the future.
		}
	        
	    }
	    for (int i=0;i<hitNums.size();i++){
		HitNum h = hitNums.get(i);
		h.transparency-=0.02;
		if (h.transparency < 0.04){
		    hitNums.remove(h);
		}
	    }
	}
	if (objects.size()==0)
	    if (song.player.isComplete() && !closed){
		song.close();
		w.removePanel(this);
		w.add(w.screen);
		w.screen.video=video;
		w.screen.mode=1;
		closed=true;
	    }
	repaint();
			
    }

    public void mouseMoved(MouseEvent e){
        mouseX = e.getX();
	mouseY = e.getY();
    }
    public void mouseDragged(MouseEvent e){
    }

    public class MAdapter extends MouseAdapter{
	public void mouseReleased(MouseEvent e){
	    int keyNum = e.getID();
	    if (keyNum == MouseEvent.MOUSE_RELEASED){
		mouseDown=false;
	    }
	}
	public void mousePressed(MouseEvent e){
	    int keyNum = e.getID();
	    if (keyNum == MouseEvent.MOUSE_PRESSED){
		mouseDown=true;
	    }
	}
	public void mouseClicked(MouseEvent e){
	    int keyNum = e.getID();
	    if (keyNum == MouseEvent.MOUSE_CLICKED){
		lastHitTime=time;
		mouseClicked=true;
	    }
	}
    }
    public class TAdapter extends KeyAdapter{
	public void keyReleased(KeyEvent e){
	    //z + x will operate as buttons 1 and 2
	    int keyNum = e.getKeyCode();
	    if (keyNum == KeyEvent.VK_Z){
		key1down = false;
	    }else if (keyNum == KeyEvent.VK_X){
		key2down = false;
		
	    }
	}
	public void keyPressed(KeyEvent e){
	    int keyNum = e.getKeyCode();
	    if (keyNum == KeyEvent.VK_Z){
		key1down = true;
		lastHitTime=time;
	    }else if (keyNum == KeyEvent.VK_X){
		key2down = true;
		lastHitTime=time;
	    }
	}
	
    }
    
     
}
