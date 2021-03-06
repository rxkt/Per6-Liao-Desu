import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class Window extends JFrame{
    //expand this soon to the titlescreen
    //titlescreen-jpanel
    //current-music-screen-jpanel
    //
    //phases:
    //open jframe -> titlescreen + musicscreen
    //loop thru all open directories in /songs/
    //open a random dir and play the mp3 file.
    //have the musicscreen report back to jframe what song is playing.
    //during the song selection screen, get the song from the titlescreen, open up to the specific song.
    //
    //OR:
    //have a jpanel dedicated to playing music? or container for less workload. but more memory usage.

    GameBoard current;
    SelectScreen screen;
    public Window(){
	int width =1000,height=600;
	//add((new GameBoard("yanaginagi-Tokohana/","test.txt")));
	Image cursorImage = Toolkit.getDefaultToolkit().createImage("default/cursor.gif");
	Point point = new Point(15,15);
	Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,point,"hi");
	setCursor(cursor);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setSize(width,height);
	setLocationRelativeTo(null);
	//setLayout(null);
	setTitle("Some 1/2-parody-crappy osu made with Java");
	setResizable(true);
	setVisible(true);
    }
    public void addPanel(String dir, String source,int circleSize,boolean video){
	current = new GameBoard(dir,source,circleSize,video);
	current.setFrame(this);
	add(current);
	current.streamMedia();
	current.repaint();

	revalidate();
	current.requestFocusInWindow();
    }
    public void addScreen(){
	screen = new SelectScreen();
	screen.w=this;
	add(screen);
	revalidate();
	screen.requestFocusInWindow();
    }
    public void removePanel(JPanel j){
	remove(j);
	revalidate();
    }
    public String toString(){
	return "Window";

    }
    public static void main(String[] args){
	Window w= new Window();
	//w.addPanel("songs/yanaginagi-Tokohana/","test.desu",70);
	//change songs to a search using java file doc
	w.addScreen();
    }
}
