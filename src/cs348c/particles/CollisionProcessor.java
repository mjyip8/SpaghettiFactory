package cs348c.particles;

import java.util.*;
import java.lang.Object;
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
        
        double getW(Particle p) {
        	return (p.isPinned())? 0.0: (1/p.m); 
        }
        
        /** Applies momentum-conserving impulses in an attempt to resolve the collision. */
        void resolveCollision()
        {
        	System.out.println("Entering resolveCollision");        	
        	Vector2d c_v = new Vector2d((1 - alpha) * Q.v.x + alpha * R.v.x, (1 - alpha) * Q.v.y + alpha * R.v.y);
			Vector2d qr = newVecAtT(Q, R, t);
        	Vector2d n0 = new Vector2d(qr.y /qr.length(), -qr.x / qr.length());

        	Vector2d v = new Vector2d(P.v.x - c_v.x, P.v.y - c_v.y);

        	
        	Vector2d n = scalMult(n0, Math.signum(scalMult(v, -1).dot(n0)));

        	System.out.println("n : (" + n.x + ", " + n.y + ")");
        	        	
        	double vn_minus = v.dot(n);
        	
        	double totalW = getW(P) + (Math.pow(1 - alpha, 2) * getW(Q)) + ((Math.pow(alpha, 2)) * getW(R));

        	double m = 0;
        	if (totalW != 0) {
        		m = 1/totalW;
        	}

        	double epsilon = .01;
        	double gamma = (1 + epsilon) * m * -vn_minus;

        	P.v = addVec(P.v, scalMult(n,               gamma * getW(P)));
        	Q.v = subVec(Q.v, scalMult(n, (1 - alpha) * gamma * getW(Q)));
        	R.v = subVec(R.v, scalMult(n,      alpha  * gamma * getW(R)));

        	System.out.println("Leaving resolveCollision");
        }

        /** 
         * Re-tests point-edge collision, and returns result (useful for verifying impulse "worked").
         */
        public PointEdgeCollision retest() 
        {
            return null;// testPointEdgeCollision(P, Q, R, dt, false);
        }
    }
        
    private static Vector2d subVec(Point2d a, Point2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d subVec(Point2d a, Vector2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d addVec(Vector2d a, Vector2d b) {
    	return new Vector2d(a.x + b.x, a.y + b.y);
    }
    
    private static Vector2d addVec(Point2d a, Vector2d b) {
    	return new Vector2d(a.x + b.x, a.y + b.y);
    }
    
    private static Vector2d scalMult(Vector2d v, double n) {
    	return new Vector2d(v.x * n, v.y * n);
    }
    
    private static Vector2d subVec(Vector2d a, Vector2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static double cp(Vector2d v1, Vector2d v2) 
    {
        double cp = (v1.x * v2.y) - (v1.y * v2.x);
        return (cp == -0.0)? 0.0 : cp;
    }
    
    
    public static double getAlpha(Vector2d pq, Vector2d rq) {
    	return (rq.x * pq.x + rq.y * pq.y)/rq.lengthSquared();
    }

    private static Vector2d newVecAtT(Particle p, Particle q, double t) {
    	return new Vector2d((p.x.x + p.v.x * t) - (q.x.x + q.v.x * t), (p.x.y + p.v.y * t) - (q.x.y + q.v.y * t));
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
     	
        	double c = cp(rq, pq);
        	double b = cp(rq_v, pq) + cp(rq, pq_v);
        	double a = cp(rq_v, pq_v);
        	    	
        	double epsilon = 0.00000001;
        	        		
        	if ((Math.abs(a) <= epsilon && Math.abs(b) <= epsilon) || ((b * b) - (4 * a * c) < 0)) {
            	if (debug)
            		System.out.println("returning null");
        		return null;
        	} else if (b * b - 4 * a * c >= 0 && Math.abs(a) > epsilon) {
        		double z = -.5 * (b + Math.signum(b) * Math.sqrt(Math.pow(b, 2) - (4 * a * c)));
        		double t1 = Math.min(z/a, c/z);
        		double t2 = Math.max(z/a, c/z);

            	if (debug)
            		System.out.println("t1 = " + t1 + ", t2 = " + t2);
            	
            	if (rq.y == 1.0 || rq.y == -1.0 ) {
            		System.out.println("t1 = " + t1 + ", t2 = " + t2);
            	}

        		if (t1 > 0 && t1 <= dt) {
        			if (debug)
        				System.out.println("***********T1 possible " + t1);
        			Vector2d c_pq = newVecAtT(p, q, t1);
        			Vector2d c_rq = newVecAtT(r, q, t1);

        			double alpha = getAlpha(c_pq, c_rq);
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 + epsilon && alpha > 0 - epsilon) {
        		    	if (debug)
        		    		System.out.println("***********Collision at " + t1);
        				return new PointEdgeCollision(p, q, r, t1, dt, alpha);
        			}
        		} else if (t2 > 0 && t2 <= dt) {
		    		if (debug)
	        			System.out.println("***********T2 possible " + t2);
		    		
        			Vector2d c_pq = newVecAtT(p, q, t2);
        			Vector2d c_rq = newVecAtT(r, q, t2);
        			
        			double alpha = getAlpha(c_pq, c_rq);
                	
        			if (debug)
        				System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 + epsilon && alpha > 0 - epsilon) {
        		    	if (debug)
        		    		System.out.println("*************Collision at " + t2);
        				return new PointEdgeCollision(p, q, r, t2, dt, alpha);
        			}
        		}
        	} else if (Math.abs(a) <= epsilon) { 	
        		double t = -c/b;
        		
        		if (t > 0 && t <= dt) {
        			Vector2d c_pq = newVecAtT(p, q, t);
        			Vector2d c_rq = newVecAtT(r, q, t);
        			
        			double alpha = getAlpha(c_pq, c_rq);
        			
                	if (debug)
                		System.out.println("alpha = " + alpha);
        			
        			if (alpha <= 1 + epsilon && alpha >= 0 - epsilon) {
        		    	if (debug)
        		    		System.out.println("***********Collision at " + t);
        				return new PointEdgeCollision(p, q, r, t, dt, alpha);
        			}
                	if (rq.y == 1.0 || rq.y == -1.0 ) {
                		System.out.println("alpha = " + alpha);   		
                	}
        		} 
        	} 
            return null; 
        }
    }
    
 
    
}