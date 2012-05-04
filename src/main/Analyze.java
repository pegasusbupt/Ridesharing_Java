package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

public class Analyze {
	public static void updateGraph(int sink, ArrayList<Integer> children, HashMap<Integer, ArrayList<Integer>> child_trips,
			HashMap<Integer, FatherTrip> father_trips, ArrayList<TripMeta> trip_meta){
		int j;
		ArrayList<Integer> li=new ArrayList<Integer>();
		li.add(sink);
		li.addAll(children);
		for (j = 0; j < li.size(); j++) {
			// update the child_trips
			int t_id = li.get(j);
			if (child_trips.containsKey(t_id)) {
				for (Integer f_id : child_trips.get(t_id)) {
					father_trips.get(f_id).benefit -= trip_meta
							.get(t_id).td;
					father_trips.get(f_id).children.remove(t_id);
					if (father_trips.get(f_id).children.size() == 0) {
						father_trips.remove(f_id);
					}
				}
				child_trips.remove(t_id);
			}
			if (father_trips.containsKey(t_id)) {
				for (Integer c_id : father_trips.get(t_id).children) {
					child_trips.get(c_id).remove(t_id);
					if (child_trips.get(c_id).size() == 0) {
						child_trips.remove(c_id);
					}
				}
				father_trips.remove(t_id);
			}
		}
	}
	
	public static HashMap<Integer, ArrayList<Integer>> optimal_filter(
			HashMap<Integer, ArrayList<Integer>> child_trips,
			HashMap<Integer, FatherTrip> father_trips,
			HashMap<String, Double> mergeable_relation,
			ArrayList<TripMeta> trip_meta, int capacity) {
		long start=System.currentTimeMillis();
		HashMap<Integer, ArrayList<Integer>> rp = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> sink_trips;
		HashSet<Integer> sink_ids;

		double min_delay;
		int sink_id, i, j;
		String pair_id;

		while (father_trips.size() > 0) {
			sink_trips = new ArrayList<Integer>();

			// generate maximal trips
			for (Integer f_id : father_trips.keySet()) {
				if (!child_trips.containsKey(f_id)) {
					sink_trips.add(f_id);
				}
			}
			sink_ids = new HashSet<Integer>();
			for (Integer c_id : child_trips.keySet()) {
				Set<Integer> intersec = new HashSet<Integer>(sink_trips);
				if (intersec.retainAll(child_trips.get(c_id))) {
					min_delay = Constants.INF;
					sink_id = -1;
					// choose the father trip that causes the minimum delay
					for (Integer f_id : intersec) {
						pair_id = String.valueOf(c_id) + "_"
								+ String.valueOf(f_id);
						if (mergeable_relation.get(pair_id) < min_delay) {
							sink_id = f_id;
							min_delay = mergeable_relation.get(pair_id);
						}
					}
					sink_ids.add(sink_id);
					if (!rp.containsKey(sink_id)) {
						rp.put(sink_id, new ArrayList<Integer>());
					}
					rp.get(sink_id).add(c_id);
				}
			}

			ArrayList<Double> distance;
			ArrayList<Integer> sorted;
			if (capacity < Constants.INF) {
				// sort by distance
				for (Integer s_id : new ArrayList<Integer>(sink_ids)) {
					if (rp.get(s_id).size() > capacity) {
						distance = new ArrayList<Double>();
						int c_id;
						for (i = 0; i < rp.get(s_id).size(); i++) {
							c_id = rp.get(s_id).get(i);
							distance.add(trip_meta.get(c_id).td);
						}
						sorted = CustomSort.sort(rp.get(s_id), distance,
								Collections.reverseOrder(new CustomizedSort()),
								capacity);
						rp.remove(s_id);
						rp.put(s_id, sorted);
					}
				}
			}

			// update father_trips and child_trips
			ArrayList<Integer> sinks = new ArrayList<Integer>(sink_ids);
			int selected;
			for (i = 0; i < sinks.size(); i++) {
				selected=sinks.get(i);
				updateGraph(selected,rp.get(selected),child_trips, father_trips, trip_meta);
			}
		}
		System.out.println("optimal filter : "+(System.currentTimeMillis() - start)/1000/60 + " minutes elapsed");
		return rp;
	}

	public static HashMap<String, Double> profileRP(
			HashMap<Integer, ArrayList<Integer>> rp,
			HashMap<String, Double> mergeable_relation,
			ArrayList<TripMeta> trip_meta) {
		HashMap<String, Double> results = new HashMap<String, Double>();

		double saved_distance = 0.0, max_no_of_passenger = 0, max_delay = 0.0, sum_delay = 0.0, no_of_saved_trip = 0.0, size, delay;
		for (Integer sink_id : rp.keySet()) {
			for (Integer c_id : rp.get(sink_id)) {
				saved_distance += trip_meta.get(c_id).td;
				delay = mergeable_relation.get(String.valueOf(c_id) + "_"
						+ String.valueOf(sink_id));
				sum_delay += delay;
				max_delay = delay > max_delay ? delay : max_delay;
			}
			size = rp.get(sink_id).size();
			no_of_saved_trip += size;
			max_no_of_passenger = max_no_of_passenger > size ? max_no_of_passenger
					: size;
		}
		double avg_no_of_passenger, avg_delay;
		avg_no_of_passenger = no_of_saved_trip / rp.size();
		avg_delay = sum_delay / no_of_saved_trip;

		results.put("saved_distance", saved_distance);
		results.put("no_of_saved_trips", no_of_saved_trip);
		results.put("avg_delay", avg_delay);
		results.put("max_no_of_passenger", max_no_of_passenger);
		results.put("avg_no_of_passenger", avg_no_of_passenger);
		results.put("max_delay", max_delay);
		return results;
	}
	
	public static HashMap<Integer, ArrayList<Integer>> greedy_strategy(
			HashMap<Integer, ArrayList<Integer>> child_trips,
			HashMap<Integer, FatherTrip> father_trips,
			ArrayList<TripMeta> trip_meta, int capacity, String option) {
		long start=System.currentTimeMillis();
		HashMap<Integer, ArrayList<Integer>> rp = new HashMap<Integer, ArrayList<Integer>>();
		
		int selected, i;
		Random rand=new Random();
		ArrayList<Integer> id;
		ArrayList<Double> attr;
		while(father_trips.size()>0){
			if(option.equals("random")){
				selected=1+rand.nextInt(father_trips.size());
			}else{
				id=new ArrayList<Integer>(father_trips.keySet());
				attr=new ArrayList<Double>();
				if(option.equals("benefit")){
					for(i=0;i<id.size();i++){
						attr.add(father_trips.get(id.get(i)).benefit);
					}
				}else{
					if(option.equals("avg_benefit")){
						for(i=0;i<id.size();i++){
							attr.add(father_trips.get(id.get(i)).benefit/father_trips.get(id.get(i)).children.size());
						}
					}else{
						if(option.equals("children_no")){
							for(i=0;i<id.size();i++){
								attr.add(father_trips.get(id.get(i)).children.size()/1.0);
							}
						}
					}
				}
				selected=CustomSort.max(id, attr);
			}
			//TODO:if edge filter is used, the codes should goes here
			ArrayList<Integer> children=father_trips.get(selected).children;
			updateGraph(selected, children, child_trips, father_trips, trip_meta);
		}
		
		System.out.println("greedy strategy : "+(System.currentTimeMillis() - start)/1000/60 + " minutes elapsed");
		return rp;
	}
	
	public static double upper_bound(HashMap<Integer, ArrayList<Integer>> child_trips,
			HashMap<Integer, FatherTrip> father_trips,
			ArrayList<TripMeta> trip_meta, int capacity){
		double upper_bound_by_child=0.0, upper_bound_by_father=0.0;
		int i=0;
		for(Integer c_id:child_trips.keySet()){
			upper_bound_by_child+=trip_meta.get(c_id).td;
		}
		int c_id;
		for(Integer f_id:father_trips.keySet()){
			for(i=0;i<capacity;i++){
				c_id=father_trips.get(f_id).children.get(i);
				upper_bound_by_father+=trip_meta.get(c_id).td;
			}
		}
		return upper_bound_by_child>upper_bound_by_father?upper_bound_by_father:upper_bound_by_child;
	}

}
