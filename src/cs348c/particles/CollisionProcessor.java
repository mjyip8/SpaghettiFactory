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
        double cp = (v1.x * v2.y) - (v1.y * v2.x);
        return (cp == -0.0)? 0.0 : cp;
    }
    
    
    public static double getAlpha(Vector2d pq, Vector2d rq) {
    	return (rq.x * pq.x + rq.y * pq.y)/rq.lengthSquared();
    }
    
    private static Vector2d subVec(Point2d a, Point2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d subVec(Vector2d a, Vector2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
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
        if(p == q || p==r) 
            return null;
        else {
        	Vector2d pq   = subVec(p.x, q.x);
        	Vector2d pq_v = subVec(p.v, q.v);
        	Vector2d rq   = subVec(r.x, q.x);
        	Vector2d rq_v = subVec(r.v, q.v);
        	
 
       	
        	double a = cp(rq, pq);
        	double b = cp(rq_v, pq) + cp(rq, pq_v);
        	double c = cp(rq_v, pq_v);
        	
        	//if (debug)
        		//System.out.println("a = " + a + ", b = " + b + ", c = " + c);
        	
        	if ((a == 0 && b == 0) || ((b * b) - (4 * a * c) < 0)) {
            	//if (debug)
            		//System.out.println("returning null");
        		return null;
        	} else if (b * b - 4 * a * c >= 0 && a != 0 && b != 0) {
        		double z = -.5 * (b + Math.signum(b) * Math.sqrt(b * b - 4 * a * c));
        		double t1 = Math.min(z/a, c/z);
        		double t2 = Math.max(z/a, c/z);
        		t1 = (t1 == -0.0)? 0.0 : t1;

            	//if (debug)
            		//System.out.println("t1 = " + t1 + ", t2 = " + t2);

        		if (t1 > 0 && t1 <= dt) {
		    		//System.out.println("***********t1 possible " + t1);

        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t1, p.x.getY() + p.v.y * t1);
        			Vector2d c_pq = new Vector2d(c_p.x - q.x.x, c_p.y - q.x.y);
        			
        			double alpha = getAlpha(c_pq, rq);
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 && alpha > 0) {
        		    	if (debug)
        		    		System.out.println("***********Collision at " + t1);
        				return new PointEdgeCollision(p, q, r, t1, dt, alpha);
        			}
        		} else if (t2 > 0 && t2 <= dt) {
		    		System.out.println("***********t2 possible " + t2);

        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t2, p.x.getY() + p.v.y * t2);
        			Vector2d c_pq = new Vector2d(c_p.x - q.x.x, c_p.y - q.x.y);
        			
        			double alpha = getAlpha(c_pq, rq);
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 && alpha > 0) {
        		    	if (debug)
        		    		System.out.println("*************Collision at " + t2);
        				return new PointEdgeCollision(p, q, r, t2, dt, alpha);
        			}
        		}
        	} else if (a == 0) { 	
        		double t = -c/b;
        		if (t > 0 && t <= dt) {

        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t, p.x.getY() + p.v.y * t);
        			Vector2d c_pq = new Vector2d(c_p.x - q.x.x, c_p.y - q.x.y);
        			
        			double alpha = getAlpha(c_pq, rq);
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 && alpha >= 0) {
        		    	if (debug)
        		    		System.out.println("***********Collision at " + t);
        				return new PointEdgeCollision(p, q, r, t, dt, alpha);
        			}
        		} 
        	} else if (b == 0) {
        		double t = c;
        		if (t > 0 && t <= dt) {

        			Vector2d c_p = new Vector2d(p.x.getX() + p.v.x * t, p.x.getY() + p.v.y * t);
        			Vector2d c_pq = new Vector2d(c_p.x - q.x.x, c_p.y - q.x.y);
        			
        			double alpha = getAlpha(c_pq, rq);
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 && alpha >= 0) {
        		    	System.out.println("***********Collision at " + t);
        				return new PointEdgeCollision(p, q, r, t, dt, alpha);
        			}
        		} 
        	}
            return null; 
        }
    }

}
