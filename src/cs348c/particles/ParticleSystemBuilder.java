package cs348c.particles;

import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.vecmath.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;


/**
 * CS 5643: Assignment #2 "Robust Collision Processing" 
 * (a.k.a. "The Spaghetti Factory")
 * <pre>
 * main() entry point class that initializes ParticleSystem, OpenGL
 * rendering, and GUI that manages GUI/mouse events. Spacebar toggles simulation advance.
 * </pre>
 * 
 * @author Doug James, January 2007 (revised Feb 2009)
 */
public class ParticleSystemBuilder implements GLEventListener
{
    private FrameExporter frameExporter;
    
    private static int N_STEPS_PER_FRAME = 30;

    /** Default graphics time step size. */
    public static final double DT = 0.01/30.;

    Color3f bgColor = new Color3f(0,0,0);

    /** Main window frame. */
    JFrame frame = null;

    private int width, height;

    /** The single ParticleSystem reference. */
    ParticleSystem PS;

    SpaghettiFactory spaghettiFactory = null;
    MeatballFactory  meatballFactory  = null;

    /** Object that handles all GUI and user interactions of building
     * Task objects, and simulation. */
    BuilderGUI     gui;

    Animator animator;


    /** Main constructor. Call start() to begin simulation. */
    ParticleSystemBuilder() 
    {
        PS  = new ParticleSystem();
        gui = new BuilderGUI();
    }

    /**
     * Builds and shows windows/GUI, and starts simulator.
     */
    public void start()
    {
        if(frame != null) return;

        frame = new JFrame("CS348C Particle System Builder");
        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        frame.add(canvas);

        animator = new Animator(canvas);
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    // Run this on another thread than the AWT event queue to
                    // make sure the call to Animator.stop() completes before
                    // exiting
                    new Thread(new Runnable() {
                            public void run() {
                                animator.stop();
                                System.exit(0);
                            }
                        }).start();
                }
            });

        frame.pack();
        frame.setSize(600,600);
        frame.setLocation(240, 0);
        frame.setVisible(true);
        animator.start();
    }



    private OrthoMap orthoMap;

    /** Maps mouse event into computational cell using OrthoMap. */
    public Point2d getPoint2d(MouseEvent e) {
        return orthoMap.getPoint2d(e);
    }

    /** GLEventListener implementation: Destroys JOGL2 renderer. */
    public void dispose(GLAutoDrawable drawable)
    {
      if (animator.isStarted()) animator.stop();
    }

    /** GLEventListener implementation: Initializes JOGL renderer. */
    public void init(GLAutoDrawable drawable) 
    {
        // DEBUG PIPELINE (can use to provide GL error feedback... disable for speed)
        //drawable.setGL(new DebugGL(drawable.getGL().getGL2()));

        GL2 gl = drawable.getGL().getGL2();
        System.err.println("INIT GL IS: " + gl.getClass().getName());

        gl.setSwapInterval(1);

        /// SETUP ANTI-ALIASED POINTS AND LINES:
        gl.glLineWidth(2);  /// YOU MAY WANT TO ADJUST THIS WIDTH
        gl.glPointSize(3f); /// YOU MAY WANT TO ADJUST THIS SIZE
        gl.glEnable(gl.GL_POINT_SMOOTH);
        gl.glHint  (gl.GL_POINT_SMOOTH_HINT, gl.GL_NICEST);
        gl.glEnable(gl.GL_LINE_SMOOTH);
        gl.glHint  (gl.GL_LINE_SMOOTH_HINT, gl.GL_NICEST);
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                
    if (drawable instanceof GLCanvas)
    {
                ((GLCanvas) drawable).addMouseListener(gui);
                ((GLCanvas) drawable).addMouseMotionListener(gui);

                ((GLCanvas) drawable).addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            gui.dispatchKey(e.getKeyChar(), e);
                        }
                });
        }
    }
    /** GLEventListener implementation */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    /** GLEventListener implementation */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) 
    {
        System.out.println("width="+width+", height="+height);
        height = Math.max(height, 1); // avoid height=0;
        
        this.width  = width;
        this.height = height;

         GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0,0,width,height);        

        /// SETUP ORTHOGRAPHIC PROJECTION AND MAPPING INTO UNIT CELL:
        gl.glMatrixMode(GL2.GL_PROJECTION);        
        gl.glLoadIdentity();                        
        orthoMap = new OrthoMap(width, height);//Hide grungy details in OrthoMap
        orthoMap.apply_glOrtho(gl);

        /// GET READY TO DRAW:
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }


    /** 
     * Main event loop: OpenGL display + simulation
     * advance. GLEventListener implementation.
     */
    public void display(GLAutoDrawable drawable) 
    {
        GL2 gl = drawable.getGL().getGL2();
        //gl.glClearColor(0,0,0,0);
        gl.glClearColor(bgColor.x, bgColor.y, bgColor.z, 0);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        /// DRAW COMPUTATIONAL CELL BOUNDARY:
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glColor3f(0.1f, 0.1f, 0.1f);
            gl.glVertex2d(0,0);        gl.glVertex2d(1,0);        gl.glVertex2d(1,1);        gl.glVertex2d(0,1);        gl.glVertex2d(0,0);
            gl.glEnd();
        }
            
        /// SIMULATE/DISPLAY HERE (Handled by BuilderGUI):
        gui.simulateAndDisplayScene(gl);

        if(frameExporter != null) {
            frameExporter.writeFrame(drawable);
        }
    }


    /** Interaction central: Handles windowing/mouse events, and building state. */
    class BuilderGUI implements MouseListener, MouseMotionListener//, KeyListener
    {
        boolean simulate = false;

        /** Current build task (or null) */
        Task task;

        JFrame  guiFrame;
        TaskSelector taskSelector = new TaskSelector();

        JToggleButton[] buttons;

        BuilderGUI() 
        {
            ButtonGroup     buttonGroup  = new ButtonGroup();
            JToggleButton[] buttons      = {new JToggleButton("Reset",           false),
                                            new JRadioButton ("Create Particle", true), 
                                            new JRadioButton ("Delete Particle", false), 
                                            new JRadioButton ("Move Particle",   false), 
                                            new JRadioButton ("Create Spring",   false),
                                            new JRadioButton ("Create Hair",     false),
                                            new JRadioButton ("Draw a Hair",     false),
                                            new JRadioButton ("Pin Constraint",  false),
                                            new JToggleButton("Start Spaghetti Factory", false) };
            this.buttons = buttons;

            guiFrame = new JFrame("Tasks");
            guiFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            //guiFrame.setLayout(new SpringLayout());
            guiFrame.setLayout(new GridLayout(buttons.length,1));

            for(int i=0; i<buttons.length; i++) {
                buttonGroup.add(buttons[i]);
                guiFrame.add(buttons[i]);
                buttons[i].addActionListener(taskSelector);
            }

            guiFrame.setSize(200,200);
            guiFrame.pack();
            guiFrame.setVisible(true);

            task = new CreateParticleTask();
        }

        /** Simulate then display particle system and any builder
         * adornments. */
        void simulateAndDisplayScene(GL2 gl)
        {
            /// TODO: OVERRIDE THIS INTEGRATOR (Doesn't use Force objects properly)
            if(simulate) {

                /// MULTIPLE STEPS OF SIZE DT (different from Assignment#1)
                int    nSteps = N_STEPS_PER_FRAME;
                double dt     = DT; 
                for(int k=0; k<nSteps; k++) {
                    if(spaghettiFactory!=null) {/// CREATE SPAGHETTI PARTICLES/FORCES, ETC.
                        spaghettiFactory.advanceTime(DT);
                    }
                    if(meatballFactory !=null) {
                        meatballFactory.advanceTime(DT);
                    }
                    PS.advanceTime(dt);/// 
                }

                /// EDGE-EDGE VALIDITY CHECK:
                if(PS.hasOverlappingSprings()) {
                    System.out.println("\n ############### OVERLAP DETECTED!!!!!!!!! \n");
                    simulate = false;
                    bgColor.set(0.3f,0,0);
                }

            }

            // Draw particles, springs, etc.
            PS.display(gl);

            if(spaghettiFactory!=null) spaghettiFactory.display(gl, width, height);
            if(meatballFactory!=null)  meatballFactory.display(gl, width, height);
            // Display Task, e.g., currently drawn spring.
            if(task != null) task.display(gl);
        }

        /**
         * ActionListener implementation to manage Task selection
         * using (radio) buttons.
         */
        class TaskSelector implements ActionListener
        {
            /** 
             * Resets ParticleSystem to undeformed/material state,
             * disables the simulation, and removes the active Task.
             */
            void resetToRest() {
                if(task != null)  task.reset();
                task = null;

                PS.reset();//synchronized
                if(spaghettiFactory!=null) spaghettiFactory.reset();
                if(meatballFactory!=null) meatballFactory.reset();
                simulate = false;
                bgColor.set(0,0,0);
            }

            /** Creates new Task objects to handle specified button action.  */
            public void actionPerformed(ActionEvent e)
            {
                String cmd = e.getActionCommand();
                System.out.println(cmd);

                resetToRest();
                if(cmd.equals("Reset")) {
                }
                else if(cmd.equals("Create Particle")){
                    task = new CreateParticleTask();
                }
                else if(cmd.equals("Delete Particle")){
                    task = new DeleteParticleTask();
                }
                else if(cmd.equals("Move Particle")){
                    task = new MoveParticleTask();
                }
                else if(cmd.equals("Create Spring")){
                    task = new CreateSpringTask();
                }
                else if(cmd.equals("Create Hair")){
                    task = new CreateHairTask();
                }
                else if(cmd.equals("Draw a Hair")){
                    task = new DrawHairTask();
                }
                else if(cmd.equals("Pin Constraint")){
                    task = new PinConstraintTask();
                }
                else if(cmd.equals("Start Spaghetti Factory")) {
                    task = null;
                    resetToRest();
                    PS = new ParticleSystem();
                    spaghettiFactory = new SpaghettiFactory(PS);/// Mmmm...
                    meatballFactory  = new MeatballFactory(PS); 
                    simulate = true;
                }
//                 else if(cmd.equals("Rigid Constraint")){
//                     task = new RigidConstraintTask();
//                 }
                else {
                    System.out.println("UNHANDLED ActionEvent: "+e);
                }
            }

            
        }


        // Methods required for the implementation of MouseListener
        public void mouseEntered (MouseEvent e) { if(task!=null) task.mouseEntered(e);  }
        public void mouseExited  (MouseEvent e) { if(task!=null) task.mouseExited(e);   }
        public void mousePressed (MouseEvent e) { if(task!=null) task.mousePressed(e);  }
        public void mouseReleased(MouseEvent e) { if(task!=null) task.mouseReleased(e); }
            public void mouseClicked (MouseEvent e) { if(task!=null) task.mouseClicked(e);  }

         // Methods required for the implementation of MouseMotionListener
         public void mouseDragged (MouseEvent e) { if(task!=null) task.mouseDragged(e);  }
         public void mouseMoved   (MouseEvent e) { if(task!=null) task.mouseMoved(e);    }

        /**
         * Handles keyboard events, e.g., spacebar toggles
         * simulation/pausing, and escape resets the current Task.
         */
        public void dispatchKey(char key, KeyEvent e)
        {
            //System.out.println("CHAR="+key+", keyCode="+e.getKeyCode()+", e="+e);
            if(key == ' ') {//SPACEBAR --> TOGGLE SIMULATE
                simulate = !simulate;
                if(simulate) {
                    task = new DragParticleTask();
                }
                else {
                    task = null;
                }
            }
            else if (key == 'r') {
                //System.out.println("ESCAPE");

                if(task != null)  task.reset();

                //Task lastTask = task;
                taskSelector.resetToRest();//sets task=null;
                buttons[0].doClick();
            }
            else if (key == 'e') {//toggle exporter
                frameExporter = ((frameExporter==null) ? (new FrameExporter()) : null);
                System.out.println("'e' : frameExporter = "+frameExporter);
            }
            else if (key == '=') {//increase nsteps
                N_STEPS_PER_FRAME = Math.max((int)(1.05*N_STEPS_PER_FRAME), N_STEPS_PER_FRAME+1);
                System.out.println("N_STEPS_PER_FRAME="+N_STEPS_PER_FRAME+
                                   ";  dt="+(float)DT+";  dtFrame="+(float)(DT*N_STEPS_PER_FRAME));
            }
            else if (key == '-') {//decrease nsteps
                int n = Math.min((int)(0.95*N_STEPS_PER_FRAME), N_STEPS_PER_FRAME-1);
                N_STEPS_PER_FRAME = Math.max(1, n);
                System.out.println("N_STEPS_PER_FRAME="+N_STEPS_PER_FRAME+
                                   ";  dt="+(float)DT+";  dtFrame="+(float)(DT*N_STEPS_PER_FRAME));
            }
        }

        /** 
         * "Task" command base-class extended to support
         * building/interaction via mouse interface.  All objects
         * extending Task are implemented here as inner classes for
         * simplicity.
         */
        abstract class Task implements MouseListener, MouseMotionListener
        {
            /** Displays any task-specific OpengGL information,
             * e.g., highlights, etc. */
            public void display(GL2 gl) {}

            // Methods required for the implementation of MouseListener
            public void mouseEntered (MouseEvent e) {}
            public void mouseExited  (MouseEvent e) {}
            public void mousePressed (MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseClicked (MouseEvent e) {}
    
            // Methods required for the implementation of MouseMotionListener
            public void mouseDragged (MouseEvent e) {}
            public void mouseMoved   (MouseEvent e) {}

            /** Override to specify reset behavior during "escape" key
             * events, etc. */
            abstract void reset();

        }
        /** Clicking task that creates particles. */
        class CreateParticleTask extends Task  
        {
            //private Particle lastCreatedParticle = null;

            public void mousePressed (MouseEvent e) {
                Point2d x0 = getPoint2d(e);
                Particle lastCreatedParticle = PS.createParticle(x0);
            }
            void reset() {}
        }

        /** Clicking task that deletes particles. */
        class DeleteParticleTask extends Task  
        {
            public void mousePressed (MouseEvent e) 
            {
                Particle deletedParticle = PS.getNearestParticle(getPoint2d(e));
                if(deletedParticle==null) return;
                PS.removeParticle(deletedParticle);
            }
            void reset() {}
        }

        /** Task to move nearest particle. */
        class MoveParticleTask extends Task  
        {
            private Particle moveParticle = null;

            /** Start moving nearest particle to mouse press. */
            public void mousePressed(MouseEvent e) 
            {
                // FIND NEAREST PARTICLE:
                Point2d cursorP = getPoint2d(e);//cursor position
                moveParticle = PS.getNearestParticle(cursorP);

                /// START MOVING (+ HIGHLIGHT):
                updatePosition(cursorP);
            }
            /** Update moved particle state. */
            private void updatePosition(Point2d newX)
            {
                if(moveParticle==null) return;
                moveParticle.setHighlight(true);
                moveParticle.x. set(newX);
                moveParticle.x0.set(newX);
            }
            /** Update particle. */
            public void mouseDragged(MouseEvent e)
            {
                Point2d cursorP = getPoint2d(e);//cursor position 
                updatePosition(cursorP);
            }

            /** Invokes reset() */
            public void mouseReleased(MouseEvent e) {
                reset();
            }

            /** Disable highlight, and nullify moveParticle. */
            void reset() {  
                if(moveParticle!=null) moveParticle.setHighlight(false);
                moveParticle = null; 
            }

            public void display(GL2 gl) {}
        }


        /** Creates inter-particle springs. */
        class CreateSpringTask extends Task  
        {
            private Particle p1 = null;
            private Particle p2 = null;
            private Point2d  cursorP = null;

            CreateSpringTask() {}

            /** Start making a spring from the nearest particle. */
            public void mousePressed(MouseEvent e) 
            {
                // FIND NEAREST PARTICLE:
                cursorP = getPoint2d(e);//cursor position
                p1 = PS.getNearestParticle(cursorP); /// = constant (since at rest)
                p2 = null;
            }

            /** Update cursor location for display */
            public void mouseDragged(MouseEvent e)
            {
                cursorP = getPoint2d(e);//update cursor position
            }

            /** Find nearest particle, and create a
             * SpringForce2Particle when mouse released, unless
             * nearest particle, p2, is same as p1. */
            public void mouseReleased(MouseEvent e) 
            {
                cursorP = getPoint2d(e);//cursor position
                p2      = PS.getNearestParticle(cursorP); /// = constant (since at rest)
                if(p1 != p2) {//make force object
                    SpringForce2Particle newForce = new SpringForce2Particle(p1, p2, PS);//params
                    PS.addForce(newForce);
                }
                /// RESET:
                p1 = p2 = null;
                cursorP = null;
            }

            /** Cancel any spring creation. */
            void reset()
            {
                p1 = p2 = null; 
                cursorP = null;
            }

            /** Draw spring-in-progress.  NOTE: created springs are
             * drawn by ParticleSystem. */
            public void display(GL2 gl) 
            {
                if(cursorP==null || p1==null) return;

                /// DRAW A LINE:
                gl.glColor3f(1,1,1);
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex2d(cursorP.x, cursorP.y);
                gl.glVertex2d(p1.x.x, p1.x.y);
                gl.glEnd();
            }
        }


        /** Runtime dragging of nearest particle using a spring
         * force. */
        class DragParticleTask extends Task  
        {
            private Particle dragParticle = null;
            private Point2d  cursorP      = null;

            private SpringForce1Particle springForce = null;

            public void mousePressed(MouseEvent e) 
            {
                // FIND NEAREST PARTICLE:
                cursorP = getPoint2d(e);//cursor position
                //dragParticle = PS.getNearestParticle(cursorP);
                dragParticle = PS.getNearestPinnedParticle(cursorP, false);//unpinned

                if(dragParticle != null) {/// START APPLYING FORCE:
                    springForce = new SpringForce1Particle(dragParticle, cursorP, PS);
                    PS.addForce(springForce);//to be removed later
                }
            }

            /** Cancel any particle dragging and forces. */
            void reset() {
                dragParticle = null;
                cursorP      = null;
                if(springForce != null)  PS.removeForce(springForce);
            }

            public void mouseDragged(MouseEvent e)
            {
                if(springForce != null) {
                    cursorP = getPoint2d(e);//cursor position 

                    /// UPDATE DRAG FORCE ANCHOR:
                    springForce.updatePoint(cursorP);
                }
            }

            public void mouseReleased(MouseEvent e) 
            {
                cursorP = null;
                dragParticle = null;

                /// CANCEL/REMOVE FORCE: 
                PS.removeForce(springForce);
            }

            public void display(GL2 gl) {}
        }


        /** Create hair task. */
        class CreateHairTask extends Task  
        {
            ArrayList<Particle> hairParticles = new ArrayList<Particle>();

            /** Create new particle. */
            public void mousePressed(MouseEvent e) 
            {
                Particle p2 = PS.createParticle(getPoint2d(e));
                if(hairParticles.size() > 0) {/// ADD STRETCH SPRING p1-p2:
                    Particle p1 = hairParticles.get(hairParticles.size()-1);
                    PS.addForce(new SpringForce2Particle(p1, p2, PS));

                    if(hairParticles.size() > 1) {/// ADD BENDING SPRING TO p0-p1-p2
                        Particle p0 = hairParticles.get(hairParticles.size()-2);
                        PS.addForce(new SpringForceBending(p0, p1, p2, PS));
                    }
                }
                hairParticles.add(p2);//finally add new particle to list
            }
            public void mouseDragged(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void display(GL2 gl) {}
            void reset() { hairParticles.clear(); }
        }

        /** Draw hair task. */
        class DrawHairTask extends Task  
        {
            ArrayList<Particle> hairParticles = new ArrayList<Particle>();

            /** Create new particle. */
            public void mousePressed(MouseEvent e) 
            {
                Particle p2 = PS.createParticle(getPoint2d(e));
                hairParticles.add(p2);//finally add new particle to list
            }
            public void mouseDragged(MouseEvent e) 
            {
                if(hairParticles.size()==0) throw new RuntimeException("hairParticles.size() was zero... HOW?!?");

                Particle p1 = hairParticles.get(hairParticles.size()-1);

                Point2d x = getPoint2d(e);
                if(x.distance(p1.x0) < 0.04)  
                    return;

                /// OTHERWISE ADD A PARTICLE
                Particle p2 = PS.createParticle(x);
                
                /// ADD STRETCH SPRING p1-p2:
                PS.addForce(new SpringForce2Particle(p1, p2, PS));

                if(hairParticles.size() > 1) {/// ADD BENDING SPRING TO p0-p1-p2
                    Particle p0 = hairParticles.get(hairParticles.size()-2);
                    PS.addForce(new SpringForceBending(p0, p1, p2, PS));
                }

                hairParticles.add(p2);//finally add new particle to list
            }
            public void mouseReleased(MouseEvent e) { hairParticles.clear(); }
            public void display(GL2 gl) {}
            void reset() { hairParticles.clear(); }
        }

        /** Toggle pin constraints. */
        class PinConstraintTask extends Task  
        {
            /** Toggle pin constraint on nearest particle. */
            public void mousePressed(MouseEvent e) 
            {
                Point2d  cursorP = getPoint2d(e);
                Particle p1 = PS.getNearestParticle(cursorP); /// = constant (since at rest)
                if(p1 != null) {// TOGGLE PIN:
                    p1.setPin( !p1.isPinned() );
                }
            }
            public void mouseDragged(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void display(GL2 gl) {}
            void reset() { }
        }

    }

    private static int exportId = -1;
    private class FrameExporter
    {
        private int nFrames  = 0;

        FrameExporter()  { 
            exportId += 1;
        }

        void writeFrame(GLAutoDrawable drawable)
        { 
            long   timeNS   = -System.nanoTime();
            String number   = Utils.getPaddedNumber(nFrames, 5, "0");
            String filename = "frames/export"+exportId+"-"+number+".png";/// BUG: DIRECTORY MUST EXIST!

            try{  
                java.io.File   file     = new java.io.File(filename);
                if(file.exists()) System.out.println("WARNING: OVERWRITING PREVIOUS FILE: "+filename);

                /// WRITE IMAGE:
                GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);
                screenshot.readPixels(drawable.getGL(), true);
                screenshot.write(file);

                timeNS += System.nanoTime();
                System.out.println((timeNS/1000000)+"ms:  Wrote image: "+filename);
                
            }catch(Exception e) { 
                e.printStackTrace();
                System.out.println("OOPS: "+e); 
            } 

            nFrames += 1;
        }
    }


    /**
     * ### Runs the ParticleSystemBuilder. ###
     */
    public static void main(String[] args) 
    {
        try{
            ParticleSystemBuilder psb = new ParticleSystemBuilder();
            psb.start();

        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("OOPS: "+e);
        }
    }
}
