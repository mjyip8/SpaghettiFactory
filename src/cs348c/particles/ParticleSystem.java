package cs348c.particles;

import java.util.*;
import javax.vecmath.*;
import com.jogamp.opengl.*;
import java.io.Serializable;
import cs348c.particles.CollisionProcessor;
import cs348c.particles.CollisionProcessor.PointEdgeCollision;


/**
 * Maintains dynamic lists of Particle and Force objects, and provides
 * access to their state for numerical integration of dynamics.
 * <pre>
 * Symplectic-Euler integrator is hard-coded, with embedded collision
 * processing code.
 * </pre>
 * 
 * @author Doug James, January 2007 (revised Feb 2009)
 */
public class ParticleSystem implements Serializable
{
    /** Current simulation time. */
    double time = 0;

    /** List of Particle objects. */
    ArrayList<Particle>   P = new ArrayList<Particle>();

    /** List of Force objects. */
    ArrayList<Force>      F = new ArrayList<Force>();

    /** Basic constructor. */
    public ParticleSystem() {  }

    /** Adds a force object (until removed) */
    public synchronized void addForce(Force f) {
        F.add(f);
    }

    /** Useful for removing temporary forces, such as user-interaction
     * spring forces. */
    public synchronized void removeForce(Force f) {
        F.remove(f);
    }

    /** Creates particle and adds it to the particle system. 
     * @param p0 Undeformed/material position. 
     * @return Reference to new Particle.
     */
    public synchronized Particle createParticle(Point2d p0) 
    {
        Particle newP = new Particle(p0);
        P.add(newP);
        return newP;
    }


    /** Removes particle and any attached forces from the ParticleSystem.
     * @param p Particle
     */
    public void removeParticle(Particle p) 
    {
        P.remove(p);
        
        ArrayList<Force> removalList = new ArrayList<Force>();
        for(Force f : F) {/// REMOVE f IF p IS USED IN FORCE
            if(f.contains(p))  removalList.add(f);
        }

        F.removeAll(removalList);
    }

    /** 
     * Helper-function that computes nearest particle to the specified
     * (deformed) position.
     * @return Nearest particle, or null if no particles. 
     */
    public synchronized Particle getNearestParticle(Point2d x)
    {
        Particle minP      = null;
        double   minDistSq = Double.MAX_VALUE;
        for(Particle particle : P) {
            double distSq = x.distanceSquared(particle.x);
            if(distSq < minDistSq) {
                minDistSq = distSq;
                minP = particle;
            }
        }
        return minP;
    }

    /** 
     * Helper-function that computes nearest particle to the specified
     * (deformed) position.
     * @return Nearest particle, or null if no particles. 
     * @param pinned If true, returns pinned particles, and if false, returns unpinned
     */
    public synchronized Particle getNearestPinnedParticle(Point2d x, boolean pinned)
    {
        Particle minP      = null;
        double   minDistSq = Double.MAX_VALUE;
        for(Particle particle : P) {
            if(particle.isPinned() == pinned) {
                double distSq = x.distanceSquared(particle.x);
                if(distSq < minDistSq) {
                    minDistSq = distSq;
                    minP = particle;
                }
            }
        }
        return minP;
    }

    /** Moves all particles to undeformed/materials positions, and
     * sets all velocities to zero. Synchronized to avoid problems
     * with simultaneous calls to advanceTime(). */
    public synchronized void reset()
    {
        for(Particle p : P)  {
            p.x.set(p.x0);
            p.v.set(0,0);
            p.f.set(0,0);
            p.setHighlight(false);
        }

        /// WORKAROUND FOR DANGLING MOUSE-SPRING FORCES AFTER PS-INTERNAL RESETS:
        ArrayList<Force> removeF = new ArrayList<Force>();
        for(Force f : F) {
            if(f instanceof SpringForce1Particle) removeF.add(f);
        }
        F.removeAll(removeF);

        time = 0;
    }

    /**
     * TESTS FOR SPRING-SPRING OVERLAP. 
     * <pre>
     * ################# 
     * DO NOT MODIFY!
     * ################# 
     * </pre>
     * @return True if there are overlaps between any unique
     * not-both-pinned springs without shared particles. 
     */
    public boolean hasOverlappingSprings()
    {
        for(Force f1 : F) { 
            if(f1 instanceof SpringForce2Particle) {
                SpringForce2Particle s1 = (SpringForce2Particle)f1;

                for(Force f2 : F) { 
                    if(f2 instanceof SpringForce2Particle) {
                        SpringForce2Particle s2 = (SpringForce2Particle)f2;

                        if(s1.overlaps(s2)) {
                            if(s1.p1.isPinned() && s1.p2.isPinned() && 
                               s1.p1.isPinned() && s1.p2.isPinned()) {
                                /// IGNORE
                            }
                            else
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private static Vector2d subVec(Point2d a, Point2d b) {
    	return new Vector2d(a.x - b.x, a.y - b.y);
    }
    
    private static Vector2d addVec(Vector2d a, Vector2d b) {
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
       
    private boolean isWithinH() {
    	return false;
    }
    
    /**
     * MAIN FUNCTION TO IMPLEMENT YOUR ROBUST COLLISION PROCESSING ALGORITHM.
     */
    public synchronized void advanceTime(double dt)
    {
        long t0 = -System.nanoTime();

        {/// GATHER BASIC FORCES (NO NEED TO MODIFY):

            /// CLEAR FORCE ACCUMULATORS:
            for(Particle p : P)  p.f.set(0,0);

            
            /// APPLY FORCES:
            for(Force force : F) 
                force.applyForce();

             // GRAVITY:
             for(Particle p : P)   p.f.y -= p.m * 5.f;

            // ADD SOME MASS-PROPORTIONAL DAMPING (DEFAULT IS ZERO)
            for(Particle p : P) 
                Utils.acc(p.f,  -Constants.DAMPING_MASS * p.m, p.v);
        }

        
        
        /// PENALTY FORCES (LAST!):
    	//test for collisions here!
        /// TODO: APPLY PENALTY FORCES BETWEEN ALL RELEVANT PARTICLE-EDGE PAIRS
        {
        	for(Particle p : P) {
        		//p.v = scalMult(p.v, .9999);
        		for (Force f: F) {	
        			if (f instanceof SpringForce2Particle) { //check if is spaghetti edge
        				// check is within .01
        				SpringForceParticleEdgeContact penalty_force = cs348c.particles.SpringForceParticleEdgeContact.isWithinH(p, ((SpringForce2Particle) f).p1, ((SpringForce2Particle) f).p2, this);
            			if (penalty_force != null) {
            				penalty_force.setDT(dt);
            				penalty_force.applyForce();
            			}        				
        			}
        		}
        	}
        }

        ///////////////////////////////////////////////
        /// SYMPLECTIC-EULER TIME-STEP w/ COLLISIONS:
        ///////////////////////////////////////////////
        ///////////////////////////////////////////////
        /// 1. UPDATE PREDICTOR VELOCITY WITH FORCES
        ///////////////////////////////////////////////
        for(Particle p : P) {
            /// APPLY PIN CONSTRAINTS (set p=p0, and zero out v):
            if(p.isPinned()) {
                p.v.set(0,0);
            }
            else {
                p.v.scaleAdd(dt/p.m, p.f, p.v); // v += dt * f/m;
            }

            /// CLEAR FORCE ACCUMULATOR
            p.f.set(0,0);
        }

        //////////////////////////////////////////////////
        /// 2. RESOLVE REMAINING COLLISIONS USING IMPULSES
        //////////////////////////////////////////////////
        {
        	double collisionsFound = 1;

	        while (collisionsFound > 0) {
	        	collisionsFound = 0;
        		for (Particle p : P) {
    				for (Force f1 : F) {
	        			if (f1 instanceof SpringForce2Particle) { 
	        				PointEdgeCollision collision = cs348c.particles.CollisionProcessor.testPointEdgeCollision(p, ((SpringForce2Particle) f1).p1, ((SpringForce2Particle) f1).p2, dt, false);
	        				if (collision != null) {
	        					collision.resolveCollision();
	        					collisionsFound++;
	        				} 
	        			}
    				}
        		}
	        }
        }

        //////////////////////////////////////////////////////////
        /// 3. ADVANCE POSITIONS USING COLLISION-FEASIBLE VELOCITY
        //////////////////////////////////////////////////////////
        for(Particle p : P) {
            p.x.scaleAdd(dt, p.v, p.x); //p.x += dt * p.v;
        }

        time += dt;

        t0 += System.nanoTime();
    }

    /**
     * Displays Particle and Force objects.
     */
    public synchronized void display(GL2 gl) 
    {
        for(Force force : F) {
            force.display(gl);
        }

        for(Particle particle : P) {
            particle.display(gl);
        }
    }

}
