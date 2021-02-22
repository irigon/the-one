package movement;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import core.Coord;
import core.Settings;
import core.SettingsError;
import input.WKTReader;
import movement.map.MapNode;
import movement.map.SimMap;


/**
 * A Movement model in which devices move between node locations read from points in a wkt file
 * 
 * @author Jose basedf on StationaryMultiPointsMovement
 */
public class BetweenPOIsMovement extends MapBasedMovement {

	private static final String POINT_FILE_S = "pointFile";
	private static final int PATH_LENGTH = 1;
	private Coord loc;
	private List<Coord> availablePoints;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public BetweenPOIsMovement(Settings s) {
		super(s);
		String fileName = s.getSetting(POINT_FILE_S);
		availablePoints = this.readPoints(fileName);
		//loc = getInitialLocation();
	}

	/**
	 * Copy constructor.
	 * @param smpm The StationaryMultiPointMovement prototype
	 */
	public BetweenPOIsMovement(BetweenPOIsMovement smpm) {
		super(smpm);
		this.availablePoints = smpm.availablePoints;
		//this.loc = getInitialLocation();
	}

	// Select a new location
	public Coord getNextPoint() {
		Coord chosen_loc;
		int chosen_idx = rng.nextInt(availablePoints.size());
		while ((chosen_loc = availablePoints.get(chosen_idx)) == this.loc) {
			chosen_idx = rng.nextInt(availablePoints.size());
		}
        return chosen_loc; 
	}
	
	/**
	 * Returns the only location of this movement model
	 * @return the only location of this movement model
	 */
	@Override
	public Coord getInitialLocation() {
		Coord new_loc = getNextPoint();
		this.loc = new_loc.clone();
		return this.loc;
	}

	/**
	 * Returns a single coordinate path (using the only possible coordinate)
	 * @return a single coordinate path
	 */
	@Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(loc.clone());
        Coord c = loc;

		for (int i=0; i<PATH_LENGTH; i++) {
            c = getNextPoint();
            p.addWaypoint(c);
        }

        this.loc = c;
        return p;
    }   



	@Override
	public BetweenPOIsMovement replicate() {
		return new BetweenPOIsMovement(this);
	}
	
	@Override
	protected void checkMapConnectedness(List<MapNode> nodes) {
		// map needs not to be connected, since it is combined out of all route maps
	}
	
	private List<Coord> readPoints(String fileName) {
		SimMap map = super.getMap();
		WKTReader reader = new WKTReader();
		boolean mirror = map.isMirrored();
		double xOffset = map.getOffset().getX();
		double yOffset = map.getOffset().getY();

		File pointFile;
		List<Coord> coords;
		try {
			pointFile = new File(fileName);
			coords = reader.readPoints(pointFile);
		} catch (IOException ioe) {
			throw new SettingsError("Couldn't read Point-data from file '" +
					fileName + " (cause: " + ioe.getMessage() + ")");
		}

		if (coords.size() == 0) {
			throw new SettingsError("Read a Point group of size 0 from " + fileName);
		}

		for (Coord c : coords) {
			if (mirror) { // mirror Points if map data is also mirrored
				c.setLocation(c.getX(), -c.getY()); // flip around X axis
			}
			// translate to match map data
			c.translate(xOffset, yOffset);
		}

		return coords;
	}
	
}
