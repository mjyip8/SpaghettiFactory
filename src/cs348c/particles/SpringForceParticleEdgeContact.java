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
    static final double H  = 0.0075;// thickness
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
        
        //calculating projection point where spring is attached
        Vector2d pe = subVec(p0.x, e1.x);
        Vector2d e = subVec(e2.x, e1.x);
	    e.normalize();
	    double t = pe.dot(e);
	    Vector2d c_p = addVec(e1.x, scalMult(e, t));
	    Vector2d c_v = new Vector2d((e2.v.x * t) + e1.v.x * (1 - t), (e2.v.y * t) + e1.v.y * (1 - t));
	    
	    //REST LENGTH
	    Vector2d v = subVec(c_p, p0.x0);
	    double L0 = v.length();
	    
	    //CURRENT LENGTH
	    Vector2d c = subVec(c_p, p0.x);
	    double L = c.length();
	    
	    c.normalize();
	    
	    double dvDot = c.dot(c_v) - c.dot(p0.v);
	    
	    double ks = Constants.PENALTY_STIFFNESS;
	    double kd = Constants.PENALTY_DAMPING;
	    c.scale(ks * ((L - L0) + kd * dvDot));
	    p0.f.add(c);
	    c.negate();
	    e2.f.add(scalMult(c, t));
	    e1.f.add(scalMult(c, 1 - t));
    }

    public void display(GL2 gl) {}

    public ParticleSystem getParticleSystem() { return PS; }

    public boolean contains(Particle p)  {
        return ((p==p0) || (p==e1) || (p==e2));
    }
    
    private static Vector2d subVec(Point2d a, Point2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d subVec(Vector2d a, Point2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d subVec(Point2d a, Vector2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d addVec(Point2d a, Vector2d b) {
    	return new Vector2d(a.x + b.x, a.y + b.y);
    }
    
    private static Vector2d scalMult(Vector2d v, double n) {
    	return new Vector2d(v.x * n, v.y * n);
    } 
    
    
    private static double getAlpha(Vector2d pq, Vector2d rq) {
    	return (rq.x * pq.x + rq.y * pq.y)/rq.lengthSquared();
    }
    
    public static SpringForceParticleEdgeContact isWithinH(Particle p, Particle q, Particle r, ParticleSystem ps) {
        if(p == q || p==r) 
            return null;
        else {
        	Vector2d pq   = subVec(p.x, q.x);
        	Vector2d rq   = subVec(r.x, q.x);        	
        	
        	double alpha = getAlpha(pq, rq);
            if (alpha > 0 && alpha < 1) {
        	    Vector2d d = scalMult(rq, 1/ rq.length());
        	    double t = pq.dot(d);
        	    Vector2d P = addVec(q.x, scalMult(d, t));
        	    Vector2d n = subVec(p.x, P);
        		return (n.length() <= H) ? new SpringForceParticleEdgeContact(p, q, r, ps) : null;        		
        	} else {
        		return null;
        	}
        }
    }
}
