package cs348c.particles;
import javax.vecmath.*;



import java.util.*;

import java.util.ArrayList;

//my unsuccessful quadTree :(
public class QuadTree {
	
	static class Node {
		public double x, y, width, height;
		private double minX = 0;
		private double minY = 0;
		private double maxX = 0;
		private double maxY = 0;
		private Node nw;
		private Node ne;
		private Node se;
		private Node sw;
		private Node nodes[];
		private ArrayList<SpringForce2Particle> kids;
		private int depth;
		private int maxDepth = 4;
		private int maxKids = 20;
		
		private Node(double x, double y, double width, double height, int depth) {
			this.width = width;
			this.height = height;
			this.x = x;
			this.y = y;
			this.minX = x;
			this.minY = y;
			this.maxX = x + width;
			this.maxY = y + width;	
			this.nw = null;
			this.ne = null;
			this.se = null;
			this.sw = null;
			this.kids = new ArrayList<SpringForce2Particle>();
			this.depth = depth;
		}
		
		private void subdivide() {
			int depth = this.depth + 1;
			double new_width = width/2;
			double new_height = height/2;
			this.nw = new Node(x            , y + new_height, new_width, new_height, depth + 1);
			this.ne = new Node(x + new_width, y + new_height, new_width, new_height, depth + 1);
			this.se = new Node(x + new_width, y             , new_width, new_height, depth + 1);
			this.sw = new Node(x            , y             , new_width, new_height, depth + 1);
			
			for (SpringForce2Particle kid : kids) {
				insertIntoKids(kid);
			}
			kids.clear();
		}
		
        private boolean isLeaf() {
            return (nw ==null && ne ==null && sw ==null && se ==null);
        }
        
        private boolean containsEdge(Particle p, Particle q) {
			Vector2d p_n = new Vector2d(p.x.x + ParticleSystemBuilder.DT * p.v.x, p.x.y + ParticleSystemBuilder.DT * p.v.y);
			Vector2d q_n = new Vector2d(q.x.x + ParticleSystemBuilder.DT * q.v.x, q.x.y + ParticleSystemBuilder.DT * q.v.y);
			
			double aabb_minX = Math.min(p.x.x, q.x.x);
			aabb_minX = Math.min(aabb_minX, p_n.x);
			aabb_minX = Math.min(aabb_minX, p_n.x);
			
			double aabb_maxX = Math.max(p.x.x, q.x.x);
			aabb_maxX = Math.max(aabb_maxX, p_n.x);
			aabb_maxX = Math.max(aabb_maxX, q_n.x);
			
			double aabb_minY = Math.min(p.x.y, q.x.y);
			aabb_minY = Math.min(aabb_minY, p_n.y);
			aabb_minY = Math.min(aabb_minY, q_n.y);
			
			double aabb_maxY = Math.max(p.x.y, q.x.y);
			aabb_maxY = Math.max(aabb_maxY, p_n.y);
			aabb_maxY = Math.max(aabb_maxY, q_n.y);

			return (aabb_minX >= minX && aabb_maxX <= maxX && aabb_minY >= minY && aabb_maxY <= maxY);    	
        }
        
        private boolean insertIntoKids(SpringForce2Particle edge) {
        	return (nw.insert(edge) || ne.insert(edge) || sw.insert(edge) || se.insert(edge));
        }
        
		public boolean insert(SpringForce2Particle edge) {
			if (!containsEdge(edge.p1, edge.p2) || (isLeaf()) && kids.contains(edge)) {
				return false;
			} else if (depth == maxDepth || (isLeaf() && kids.size() < maxKids)) {
				kids.add(edge);
				return true;
			} else if (isLeaf() && depth < maxDepth) {
				subdivide();
				return insertIntoKids(edge);
			}
			return false;
		}
		
		public ArrayList<SpringForce2Particle> getKids() {
			ArrayList<SpringForce2Particle> result = kids;
			return result;
		}
		
		public ArrayList<SpringForce2Particle> getAllInfo() {
			ArrayList<SpringForce2Particle> result = new ArrayList<SpringForce2Particle>();
			for (Node n : nodes) {
				result.addAll(n.getAllInfo());
			}
			result.addAll(kids);
			return result;
		}
		
		
		
		/*private boolean intersectsBox(SpringForce2Particle edge) {
			Particle p = edge.p1;
			Particle q = edge.p2; 
			
			double n_minX = Math.min(n.x.x, n.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.x));
			double n_maxX = Math.max(n.x.x, n.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.x));

			double p_minY = Math.min(n.x.y, n.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.y));
			double p_maxY = Math.max(n.x.y, n.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.y));
			if (p_minX == p_maxX) {
				p_minX -= (p_minX == 0.01)? 0 : .01;
				p_maxX += (p_maxX == .99)? 0 : .01;
			}		
			if (p_minY == p_maxY) {
				p_minY -= (p_minY < 0.01)? 0 : .01;
				p_maxY += (p_maxY > .99)? 0 : .01;
			}	
			
			
			if (p_minX >= minX && p_maxX <= maxX && p_minY >= minY && p_maxY <= maxY) {
				return true;
			}
			
			if (p_maxX < minX || p_minX > maxX || p_maxY < minY || p_minY > maxY) return false;
			
			return true;
		}*/
		
		private double[] getBoundingBox(SpringForce2Particle edge, boolean padding) {
			double[] result = new double[4];
			Particle n = edge.p1;
			Particle m = edge.p2;
			double h = 0.01;
			
			double p_minX = Math.min(n.x.x, n.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.x));
			p_minX = Math.min(Math.min(p_minX, m.x.x), m.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((m.isPinned()) ? 0 : m.v.x));
			double p_maxX = Math.max(n.x.x, n.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.x));
			p_maxX = Math.max(Math.max(p_maxX, m.x.x), m.x.x + cs348c.particles.ParticleSystemBuilder.DT * ((m.isPinned()) ? 0 : m.v.x));
			
			p_minX = (padding && p_minX >= h)? p_minX - h : p_minX;
			p_maxX = (padding && p_maxX <= 1 - h)? p_maxX + h : p_maxX;
			
			double p_minY = Math.min(n.x.y, n.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.y));
			p_minY = Math.min(Math.min(p_minY, m.x.y), m.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((m.isPinned()) ? 0 : m.v.y));
			double p_maxY = Math.max(n.x.y, n.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((n.isPinned()) ? 0 : n.v.y));
			p_maxY = Math.max(Math.max(p_maxY, m.x.y), m.x.y + cs348c.particles.ParticleSystemBuilder.DT * ((m.isPinned()) ? 0 : m.v.y));
			
			p_minY = (padding && p_minY >= h)? p_minY - h : p_minY;
			p_maxY = (padding && p_maxY <= 1 - h)? p_maxY + h : p_maxY;
			
			result[0] = p_minX;
			result[1] = p_maxX;
			result[2] = p_minY;
			result[3] = p_maxY;
			return result;
		}
		
		private boolean overlapping1d(double[] arr1, double[] arr2) {
			return arr1[1] >= arr2[0] && arr2[1] >= arr1[0];
		}
		
		private boolean EdgeIntersection(SpringForce2Particle edge1, SpringForce2Particle edge2, boolean padding) {
			double[] box1 = getBoundingBox(edge1, padding);
			double[] box2 = getBoundingBox(edge2, padding);
			return overlapping1d(Arrays.copyOfRange(box1, 0, 2), Arrays.copyOfRange(box2, 0, 2)) && 
					overlapping1d(Arrays.copyOfRange(box1, 2, 4), Arrays.copyOfRange(box2, 2, 4));
		}
		
		
		
		public Set<SpringForce2Particle> getClosestEdges(SpringForce2Particle edge, boolean padding) {
			Set<SpringForce2Particle> result = new HashSet<SpringForce2Particle>();
			
			if (!containsEdge(edge.p1, edge.p2)) return result;
			
			if (isLeaf()) {
				for (SpringForce2Particle kid : kids) {
					if (EdgeIntersection(edge, kid, padding)) {
						result.add(kid);
					}
				}	
				return result;
			} 
			
			result.addAll(nw.getClosestEdges(edge, padding));
			result.addAll(ne.getClosestEdges(edge, padding));
			result.addAll(sw.getClosestEdges(edge, padding));
			result.addAll(se.getClosestEdges(edge, padding));
			return result;
		}
		
		public void clear() {
			kids.clear();
			nw.clear();
			sw.clear();
			ne.clear();
			nw.clear();
		}		
	}
	
	Node root;
	
	
	public QuadTree(double x, double y, double width, double height, int maxDepth) {
		this.root = new Node(x, y, width, height, 1);

	}
	
	public void insert(SpringForce2Particle edge) {
		this.root.insert(edge);
	}
	
	public Set<SpringForce2Particle> getClosestEdges(SpringForce2Particle edge, boolean padding) {
		Set<SpringForce2Particle> result = this.root.getClosestEdges(edge, padding);
		System.out.println(result);
		return result;
	}
	
	public void clear() {
		//this.root.clear();
	}
}
