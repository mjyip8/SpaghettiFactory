package cs348c.particles;

import javax.vecmath.*;
import com.jogamp.opengl.*;

/** 
 * Spring force between two particles, with spring-spring overlap tests.
 * 
 * @author Doug James, January 2007 (Revised Feb 2009)
 */
public class SpringForce2Particle implements Force
{
    Particle p1;
    Particle p2;
    ParticleSystem PS;
    private boolean isMeatball = false;

    SpringForce2Particle(Particle p1, Particle p2, ParticleSystem PS)
    {
        if(p1==null || p2==null) throw new NullPointerException("p1="+p1+", p2="+p2);

        this.p1 = p1;
        this.p2 = p2;
        this.PS = PS;
    }

    SpringForce2Particle(Particle p1, Particle p2, ParticleSystem PS, boolean isMeatball)
    {
        this(p1, p2, PS);
        this.isMeatball = isMeatball;
    }

//     SpaceTimeBound getSpaceTimeBound(double dt)
//     {
//         return new SpaceTimeBound(dt);
//     }

    /** 
     * Returns true if this spring overlaps the specified
     * spring. Returns false if the any particles are shared. 
     * 
     * ### DO NOT MODIFY ###
     */
    public boolean overlaps(SpringForce2Particle S)
    {
        /// RETURN FALSE IF ANY PARTICLES SHARED:
        if(S == this)            return false;
        if(p1==S.p1 || p1==S.p2) return false;
        if(p2==S.p1 || p2==S.p2) return false;

        return overlaps(p1.x, p2.x, S.p1.x, S.p2.x);
    }

    private static boolean overlaps(Point2d p1, Point2d p2, 
                                    Point2d q1, Point2d q2)
    {
        /// CONSTRUCT 2x2 LINEAR SYSTEM FOR (alpha,beta) CONVEX COORDS:
        /// [ a b ] [alpha] = [c]
        /// [ d e ] [beta ]   [f]
        double a =  (p2.x-p1.x);
        double d =  (p2.y-p1.y);
        double b = -(q2.x-q1.x);
        double e = -(q2.y-q1.y);
        double c =  (q1.x-p1.x);
        double f =  (q1.y-p1.y);

        /// SOLVE, e.g., USING CRAMER'S RULE:
        double det   = a*e - b*d;
        if(det==0) return false;//BUG?
        //if(det==0) throw new MathException("det==0 during Cramer's rule solve--lines are parallel");
        double alpha = (c*e-b*f) / det;
        double beta  = (a*f-c*d) / det;

        return (alpha>=0 && alpha<=1 && beta>=0 && beta<=1);
    }

    public void applyForce()
    {
        if(p1.isPinned() && p2.isPinned()) return;/// no force

        {
            Vector2d v = new Vector2d();

            /// REST LENGTH:
            v.sub(p2.x0, p1.x0);
            double L0 = v.length();

            /// CURRENT LENGTH:
            v.sub(p2.x, p1.x);
            double L = v.length();

            v.normalize();

            // DAMPING: dv-dot-dpHat
            double dvDot = v.dot(p2.v) - v.dot(p1.v);

            double k = Constants.STIFFNESS_STRETCH;
            v.scale( k * ((L-L0)  + 0.1*dvDot ) );

            p1.f.add(v);
            v.negate();
            p2.f.add(v);

        }
    }

    public void display(GL2 gl)
    {
        if( !isMeatball)
                gl.glColor3f(1f, 239f/255f, 160f/255f);//spaghetti color
        else
                gl.glColor3f(139f/255f, 69f/255f, 0f/255f);//meatball color

        gl.glBegin(GL2.GL_LINES);
        gl.glVertex2d(p1.x.x, p1.x.y);
        gl.glVertex2d(p2.x.x, p2.x.y);
        gl.glEnd();        
    }

    public ParticleSystem getParticleSystem() { return PS; }

    public boolean contains(Particle p)  { 
        return ((p==p1) || (p==p2));
    }
}
