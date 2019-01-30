package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;

import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;

/**
 * Implements a "spaghetti factory" consisting of a pinned bounding
 * box with an inlet that shoots in numerous spaghetti strands. 
 * <pre>
 * ################################################
 * DO NOT EDIT ANY OF THIS CODE FOR THE ASSIGNMENT.
 * ################################################
 * </pre>
 * @author Doug James, February 2009.
 */
public class SpaghettiFactory
{
    private static final int N_PARTICLES_PER_STRAND = 13;/// LUCKY LUCKY ;)

    private static final Point2d INLET = new Point2d(0.999, 0.9975);

    private ParticleSystem PS;

    private double time;

    /** Speed of spaghetti at inlet. */
    private double speed = 1;

    /// Spacing between particles, h:
    private double   h   = 0.05;

    private Particle p0, p1;

    /// Time of last particle emission
    private double   tLast;

    private int nEdges   = 0;

    /** List of spaghetti Particle objects. */
    private ArrayList<Particle>   P = new ArrayList<Particle>();

    /** List of spaghetti Force objects. */
    private ArrayList<Force>      F = new ArrayList<Force>();

    private TextRenderer textRenderer;

    /** Creates an empty pinned-box boundary, and prepares for
     * spaghetti emission. */
    SpaghettiFactory(ParticleSystem PS) 
    {
        this.PS = PS;

        /// BUILD BOX
        double e = 0.00;  
        Particle p00 = PS.createParticle(new Point2d(  e,  e));  p00.setPin(true);
        Particle p01 = PS.createParticle(new Point2d(  e,1-e));  p01.setPin(true);
        Particle p11 = PS.createParticle(new Point2d(1-e,1-e));  p11.setPin(true);
        Particle p10 = PS.createParticle(new Point2d(1-e,  e));  p10.setPin(true);
        PS.addForce(new SpringForce2Particle(p00, p01, PS));
        PS.addForce(new SpringForce2Particle(p00, p10, PS));
        PS.addForce(new SpringForce2Particle(p11, p10, PS));
        PS.addForce(new SpringForce2Particle(p11, p01, PS));

        /// BUILD TextRenderer
        textRenderer = new TextRenderer(new Font("Monospaced", Font.BOLD, 16), true, true); //SansSerif ,,, Dialog
    }

    /** Deletes all previous spaghetti particles and forces, and
     * restarts factory. */
    public void reset() 
    {
        /// REMOVE SPAGHETTI PARTICLES ...
        for(Particle p : P)  PS.removeParticle(p);
        P.clear();

        /// ... and FORCES:
        for(Force f : F)  PS.removeForce(f);
        F.clear();
        nEdges = 0;

        p0 = null; // i-0 particle on current strand
        p1 = null; // i-1 particle on current strand

        time = 0;
    }

    /** Creates new particles and forces, and squirts spaghetti into
     * the box. Does not advance positions---done elsewhere by
     * ParticleSystem integrator. */
    public void advanceTime(double dt)
    {
        if(p0==null) {
            p0 = PS.createParticle(new Point2d(INLET));
            p1 = null;
            P.add(p0);
            p0.x.set(INLET);
            p0.v.set(-speed, 0);
            tLast = time;
        }

        if(time - tLast > h/speed) {/// MAKE NEW PARTICLE:
            Particle p = PS.createParticle(new Point2d(INLET.x+P.size()*h, INLET.y));
            P.add(p);
            p.x.set(INLET);
            p.v.set(-speed, 0);

            // CONNECT SPRING:
            if(P.size()%N_PARTICLES_PER_STRAND != 0) {
                Force f = new SpringForce2Particle(p, p0, PS);
                PS.addForce(f);
                F.add(f);
                nEdges++;

                if(p1 != null) {// ADD BENDING FORCE:
                    f = new SpringForceBending(p, p0, p1, PS);
                    PS.addForce(f);
                    F.add(f);
                }
            }
            else {
                p0 = p1 = null;
            }

            // ADJUST VELOCITIES TO KEEP FROM FALLING TOO FAST:
            if(p0 != null)  p0.v.set(-speed, 0);
            if(p1 != null)  p1.v.set(-speed, 0);

            // UPDATE REFERENCES:
            p1 = p0;
            p0 = p;

            tLast = time;
        }
        else {
            // ADJUST VELOCITIES TO KEEP FROM FALLING TOO FAST:
            if(p0 != null)  p0.v.set(-speed, 0);
            if(p1 != null)  p1.v.set(-speed, 0);
        }

        time += dt;
    }

    /** Number of complete strands of spaghetti. */
    public int getNSpaghetti() { 
        return P.size()/N_PARTICLES_PER_STRAND;
    }

    /** Displays spaghetti text stats. */
    public void display(GL2 gl, int width, int height)
    {
        textRenderer.beginRendering(width, height);
        {
            // optionally set the color
            textRenderer.setColor(0.9f, 0.9f, 0.9f, 1f);

            // BUG: Text position should be in relative coords for resizing
            textRenderer.draw(  "Spaghetti: "          +getNSpaghetti(), 20, height-34);
            textRenderer.draw(  "Spaghetti Particles: "+P.size()       , 20, height-54);
            textRenderer.draw(  "Spaghetti Edges    : "+nEdges         , 20, height-74);
            // ... more draw commands, color changes, etc.
        }
        textRenderer.endRendering();
    }
    

}
