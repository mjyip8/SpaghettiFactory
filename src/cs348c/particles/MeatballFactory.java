package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;

import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

/**
 * Implements a "meatball factory" that drops meatballs randomly from above
 *    the box
 * <pre>
 * ################################################
 * DO NOT EDIT ANY OF THIS CODE FOR THE ASSIGNMENT.
 * ################################################
 * </pre>
 * @author Doug James, February 2009.
 */
public class MeatballFactory
{
    private static final int N_PARTICLES_PER_BALL = 3;
    private static final int MAX_N_MEATBALLS = 16;

    private Point2d INLET    = new Point2d(0.5, 0.9);

    private ParticleSystem PS;

    private double time;

    /** downward of meatball at inlet. */
    private double speed    = 0.07;
    /** angular velocity of meatball at inlet **/
    private double angspeed = 15;
    /** edge length of meatball at inlet **/
    private double size     = 0.08;
    /// vertical spacing between meatballs, h:
    private double   h      = 0.08;

    /// Time of last meatball emission
    private double   tLast  = -h/speed;

    private int nEdges      = 0;

    /** List of meatball Particle objects. */
    private ArrayList<Particle>   P = new ArrayList<Particle>();

    /** List of meatball Force objects. */
    private ArrayList<Force>      F = new ArrayList<Force>();

    private TextRenderer textRenderer;

    /** Creates an empty pinned-box boundary, and prepares for
     * meatball emission. */
    MeatballFactory(ParticleSystem PS) 
    {
        this.PS = PS;

        /// BUILD TextRenderer
        textRenderer = new TextRenderer(new Font("Monospaced", Font.BOLD, 16), true, true); //SansSerif ,,, Dialog
    }

    /** Deletes all previous meatball particles and forces, and
     * restarts factory. */
    public void reset() 
    {
        /// REMOVE MEATBALL PARTICLES ...
        for(Particle p : P)  PS.removeParticle(p);
        P.clear();

        /// ... and FORCES:
        for(Force f : F)  PS.removeForce(f);
        F.clear();
        nEdges = 0;

        INLET.x = 0.5;
        if (angspeed < 0) angspeed = -angspeed;
        time = 0;
    }

    /** Creates new particles and forces, and squirts meatball into
     * the box. Does not advance positions---done elsewhere by
     * ParticleSystem integrator. */
    public void advanceTime(double dt)
    {

        if(time - tLast > h/speed && getNMeatball() < MAX_N_MEATBALLS)
        {/// MAKE NEW PARTICLE:
            Particle p1 = PS.createParticle(new Point2d(INLET.x, INLET.y + size * 0.57735026919));
            Particle p2 = PS.createParticle(new Point2d(INLET.x + 0.5 * size, INLET.y - size * 0.28867513459));
            Particle p3 = PS.createParticle(new Point2d(INLET.x - 0.5 * size, INLET.y - size * 0.28867513459));
    
            // CONNECT MEATBALL
            SpringForce2Particle f1 = new SpringForce2Particle(p1, p2, PS, true /* isMeatball */);
            SpringForce2Particle f2 = new SpringForce2Particle(p2, p3, PS, true /* isMeatball */);
            SpringForce2Particle f3 = new SpringForce2Particle(p3, p1, PS, true /* isMeatball */);
            boolean overlaps = false;
            for (Force f : PS.F)
                if( f instanceof SpringForce2Particle )
                {
                    SpringForce2Particle s = (SpringForce2Particle) f;
                    if (overlaps = (s.overlaps(f1) || s.overlaps(f2) || s.overlaps(f3)))
                        break;
                }
                
            if( overlaps )
            {
                PS.removeParticle(p1);
                PS.removeParticle(p2);
                PS.removeParticle(p3);
            }
            else
            {
                p1.v.set(0, -speed);
                p2.v.set(0, -speed);
                p3.v.set(0, -speed);
                Utils.acc(p1.v, angspeed, new Point2d(-(p1.x.y - INLET.y), (p1.x.x - INLET.x)));
                Utils.acc(p2.v, angspeed, new Point2d(-(p2.x.y - INLET.y), (p2.x.x - INLET.x)));
                Utils.acc(p3.v, angspeed, new Point2d(-(p3.x.y - INLET.y), (p3.x.x - INLET.x)));
                PS.addForce(f1);
                PS.addForce(f2);
                PS.addForce(f3);
                P.add(p1);
                P.add(p2);
                P.add(p3);
                F.add(f1);
                F.add(f2);
                F.add(f3);
                nEdges++;
                nEdges++;
                nEdges++;
                tLast = time;
            }
    
            // UPDATE REFERENCES:
    
            INLET.x = INLET.x + 0.16;
            if( INLET.x > 0.9 )
                INLET.x = INLET.x - 0.8;
            angspeed = -angspeed;
        }

        time += dt;
    }

    /** Number of complete strands of meatball. */
    public int getNMeatball() { 
        return P.size()/N_PARTICLES_PER_BALL;
    }

    /** Displays meatball text stats. */
    public void display(GL2 gl, int width, int height)
    {
        textRenderer.beginRendering(width, height);
        {
            // optionally set the color
            textRenderer.setColor(0.9f, 0.9f, 0.9f, 1f);

            // BUG: Text position should be in relative coords for resizing
            textRenderer.draw(  "Meatball: "          +getNMeatball() , 20, height-94);
            textRenderer.draw(  "Meatball Particles: "+P.size()       , 20, height-114);
            textRenderer.draw(  "Meatball Edges    : "+nEdges         , 20, height-134);
            // ... more draw commands, color changes, etc.
        }
        textRenderer.endRendering();
    }
    

}
