/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 * The Original PRoPHET code updated to PRoPHETv2 router -- this version is just an adaptation
 * by Samo Grasic(samo@grasic.net) - Jun 2011
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Random;

import routing.util.RoutingInfo;


import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import util.Tuple;

/**
 * Implementation of PRoPHET" with Time Window (PTW)
 */
public class PTWRouter extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double PEncMax = 0.5;
	/** typical interconnection time in seconds*/
	public static final double I_TYP = 1800;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.9;
	/** delivery predictability aging constant */
	public static final double DEFAULT_GAMMA = 0.999885791;
	/** pedestrians communication window **/
	public static final double[] DEF_PEDESTRIAN_ACTIVE_WINDOW = {0,Double.MAX_VALUE};
	public static final String PEDESTRIAN_ACTIVE_WINDOW = "active_window";
	/** flag to define whether the router adapts **/
	public static final String ADAPTIVE_ROUTING = "adaptive_routing";

	Random randomGenerator = new Random();

	/** PTW router's setting namespace ({@value})*/
	public static final String PTW_NS = "PTW";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/**
	 * Predictability aging constant (gamma) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_GAMMA}.
	 */
	public static final String GAMMA_S = "gamma";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;
	/** value of gamma setting */
	private double gamma;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;

	/** last encouter timestamp (sim)time */
	private Map<DTNHost, Double> lastEncouterTime;

	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;
	
	/** Period within wichi persons (P group) communicate **/
	double[] p_active_window;
	protected double begin_period;
	protected double end_period;
	
	private boolean adaptive_routing;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public PTWRouter(Settings s) {
		super(s);
		Settings PTWSettings = new Settings(PTW_NS);
		secondsInTimeUnit = PTWSettings.getInt(SECONDS_IN_UNIT_S);
		if (PTWSettings.contains(BETA_S)) {
			beta = PTWSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}
		if (PTWSettings.contains(GAMMA_S)) {
			gamma = PTWSettings.getDouble(GAMMA_S);
		}
		else {
			gamma = DEFAULT_GAMMA;
		}

		if (PTWSettings.contains(PEDESTRIAN_ACTIVE_WINDOW)) {
			p_active_window = PTWSettings.getCsvDoubles(PEDESTRIAN_ACTIVE_WINDOW, 2);
		}
		else {
			p_active_window = DEF_PEDESTRIAN_ACTIVE_WINDOW;
		}

		if (PTWSettings.contains(ADAPTIVE_ROUTING)) {
			adaptive_routing = PTWSettings.getBoolean(ADAPTIVE_ROUTING);
		}

		
		begin_period = p_active_window[0];
		end_period = p_active_window[1];
        System.out.println("Begin Period:" + begin_period + ", end period: " + end_period);
		
		initPreds();
		initEncTimes();
	}

	/**
	 * Copyc onstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected PTWRouter(PTWRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		this.gamma = r.gamma;
		this.begin_period = r.begin_period;
		this.end_period = r.end_period;
		this.adaptive_routing = r.adaptive_routing;

		initPreds();
		initEncTimes();
	}

	/**
	 * Initializes lastEncouterTime hash
	 */
	private void initEncTimes() {
		this.lastEncouterTime = new HashMap<DTNHost, Double>();
	}

		/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			if (adaptive_routing && !otherHost.is_pedestrian()) {
				updateDeliveryPredFor(otherHost);
				updateTransitivePreds(otherHost);
			}
		}
	}

	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * PEnc
	 * PEnc(intvl) =
     *        P_encounter_max * (intvl / I_typ) for 0<= intvl <= I_typ
     *        P_encounter_max for intvl > I_typ</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double PEnc;
		double simTime = SimClock.getTime();
		double lastEncTime=getEncTimeFor(host);
		if(lastEncTime==0)
			PEnc=PEncMax;
		else
			if((simTime-lastEncTime)<I_TYP)
			{
				PEnc=PEncMax*((simTime-lastEncTime)/I_TYP);
			}
			else
				PEnc=PEncMax;

		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * PEnc;
		preds.put(host, newValue);
		lastEncouterTime.put(host, simTime);
	}

	/**
	 * Returns the timestamp of the last encouter of with the host or -1 if
	 * entry for the host doesn't exist.
	 * @param host The host to look the timestamp for
	 * @return the last timestamp of encouter with the host
	 */
	public double getEncTimeFor(DTNHost host) {
		if (lastEncouterTime.containsKey(host)) {
			return lastEncouterTime.get(host);
		}
		else {
			return 0;
		}
	}

		/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof PTWRouter :
			"PTW only works with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds =
			((PTWRouter)otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pForHost * e.getValue() * beta;
			if(pNew>pOld)
				preds.put(e.getKey(), pNew);

		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
			secondsInTimeUnit;

		if (timeDiff == 0) {
			return;
		}

		double mult = Math.pow(gamma, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}
	
	public boolean pedestrian_active_period() {
		return (SimClock.getTime() > begin_period && SimClock.getTime() < end_period );
	}

	@Override
	public void update() {
		
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		// in adaptive routing, during the period that pedestrians are active, stations flood.
		if (this.getHost().is_pedestrian() || (adaptive_routing && pedestrian_active_period())) {
			// EPIDEMIC
			this.tryAllMessagesToAllConnections();
		// PTN --> PTW
		} else { 	 
			tryOtherMessages();
		}
	}

	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages =
			new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			PTWRouter othRouter = (PTWRouter)other.getRouter();

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if((othRouter.getPredFor(m.getTo()) >= getPredFor(m.getTo())))
				{

					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}

	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((PTWRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((PTWRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() +
				" delivery prediction(s)");

		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
					host, value)));
		}

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		PTWRouter r = new PTWRouter(this);
		return r;
	}
}
