package cs348c.particles;

import javax.vecmath.*;
import com.jogamp.opengl.*;

/** 
 * Penalty force to model particle-edge contact.
 * <pre>
 * SKELETON IMPLEMENTATION OF PARTICLE-EDGE PENALTY FORCE. FEEL FREE
 * TO MODIFY OR IGNORE.
 * </pre>
 * @author Doug James, February 2009.
 */
public class SpringForceParticleEdgeContact implements Force
{
    static final double H  = 0.01;// thickness
    static final double H2 = H*H;

    private Particle p0;
    private Particle e1;
    private Particle e2;
    private ParticleSystem PS;
    private double dt = -1;

    /** Constructor with null references.  Must invoke setDT() and
     * update() before use. */
    SpringForceParticleEdgeContact() { } 

    SpringForceParticleEdgeContact(Particle p0, Particle e1, Particle e2, ParticleSystem PS)
    {
        this.PS = PS;
        update(p0,e1,e2);
    }

    /** Specify timestep size to evaluate Bridson penalty force. */
    void setDT(double dt) {
        this.dt = dt;
    }

    /** Set particle, p0, and edge (e1,e2) references.  */
    void update(Particle p0, Particle e1, Particle e2)
    {
        if(p0==null || e1==null || e2==null) throw new NullPointerException("p0,e1,e2 null");

        if(p0==e1 || p0==e2) throw new IllegalArgumentException("edge can't contact itself");

        this.p0 = p0;
        this.e1 = e1;
        this.e2 = e2;
    }

    /** YOUR IMPLEMENTATION of the penalty contact force. */
    public void applyForce()
    {
        if(p0.isPinned() && e1.isPinned() && e2.isPinned()) return;/// no force

        /// OTHERWISE COMPUTE PROXIMITY AND ANY FORCE: 
        /// 
        /// <INSERT YOUR IMPLEMENTATION HERE>
        /// .... ;)    
    }

    public void display(GL2 gl) {}

    public ParticleSystem getParticleSystem() { return PS; }

    public boolean contains(Particle p)  {
        return ((p==p0) || (p==e1) || (p==e2));
    }
}
