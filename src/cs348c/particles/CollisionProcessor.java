package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.GL2;


/**
 * A class to put your particle-edge collision detection and resolution implementation.
 * <pre>
 * SKELETON CODE IS INCLUDED, BUT CAN BE REMOVED/IGNORED IF DESIRED.
 * </pre>
 * @author Doug James, February 2009
 */
public class CollisionProcessor 
{
    /** 
     * Representation of a point-edge collision event requiring impulse resolution.
     */
    static class PointEdgeCollision
    {

         private PointEdgeCollision(Particle P, 
                                    Particle Q, Particle R, 
                                    double t, // <<< collision time, t* on (0,dt]
                                    double dt,
                                    double alpha)
        {
             this.P = P;
             this.Q = Q;
             this.R = R;
             this.t = t;              
             if(t <= 0) throw new IllegalArgumentException("Can't have t="+t);
             this.dt = dt;
             this.alpha = alpha;
        }
         
        public Particle P;
        public Particle Q;
        public Particle R;
        public double t;
        public double dt;
        public double alpha;

        /** Applies momentum-conserving impulses in an attempt to resolve the collision. */
        void resolveCollision()
        {
            /// COMPUTE/APPLY IMPULSE HERE...
        }

        /** 
         * Re-tests point-edge collision, and returns result (useful for verifying impulse "worked").
         */
        public PointEdgeCollision retest() 
        {
            return null;// testPointEdgeCollision(P, Q, R, dt, false);
        }
    }
    
    private static double cp(Vector2d v1, Vector2d v2) 
    {
        return (v1.x * v2.y) - (v1.y * v2.x);
    }
    
    
    public static double getAlpha(Vector2d pq, Vector2d rq) {
    	return (rq.x * pq.x + rq.y * pq.y)/rq.lengthSquared();
    }
    
    /**
     * Checks for collision between Particle p, and the edge (q,r) on
     * the interval (0,dt] given their current position and velocity.
     * 
     * @return PointEdgeCollision for first collision on the interval
     * (0, dt], or null if no collision occurs on that interval.
     */
    public static PointEdgeCollision testPointEdgeCollision(Particle p, 
                                                            Particle q, Particle r, 
                                                            double   dt, boolean debug)
    {
    	if (debug)
    		System.out.println("Entering testPointEdge");
        if(p==q || p==r) 
            return null;
        else {
        	Vector2d pq = new Vector2d((p.x.getX() - q.x.getX()), p.x.getY() - q.x.getY());
        	Vector2d pq_v = new Vector2d((p.v.x - q.v.x), p.v.y - q.v.y);
        	Vector2d rq = new Vector2d(r.x.getX() - q.x.getX(), r.x.getY() - q.x.getY());
        	Vector2d rq_v = new Vector2d((r.v.x - q.v.x), r.v.y - q.v.y);

        	
        	double a = cp(rq, pq);
        	double b = cp(rq_v, pq) + cp(rq, pq_v);
        	double c = cp(rq_v, pq_v);
        	
        	if ((a == 0 && b == 0) || (b * b - 4 * a * c < 0)) {
        		return null;
        	} else if (b * b - 4 * a * c > 0) {
        		double z = -.5 * (b + Math.signum(b) * Math.sqrt(b * b - 4 * a * c));
        		double t1 = Math.min(z/a, c/z);
        		double t2 = Math.max(z/a, c/z);
        		
        		if (t1 > 0 && t1 <= dt) {
        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t1, p.x.getY() + p.v.y * t1);
        			Vector2d c_pq = new Vector2d(c_p.x - q.v.x, c_p.y - q.v.y);
        			
        			double alpha = getAlpha(c_pq, rq);
        			if (alpha <= 1 && getAlpha(c_pq, rq) >= 0) {
        		    	if (debug)
        		    		System.out.println("Collision at " + t1);
        				return new PointEdgeCollision(p, q, r, t1, dt, alpha);
        			}
        		} else if (t2 > 0 && t2 <= dt) {
        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t2, p.x.getY() + p.v.y * t2);
        			Vector2d c_pq = new Vector2d(c_p.x - q.v.x, c_p.y - q.v.y);
        			
        			double alpha = getAlpha(c_pq, rq);
        			if (alpha <= 1 && getAlpha(c_pq, rq) >= 0) {
        		    	if (debug)
        		    		System.out.println("Collision at " + t2);
        				return new PointEdgeCollision(p, q, r, t2, dt, alpha);
        			}
        		}
        	}    	
            return null; 
        }
    }

}
