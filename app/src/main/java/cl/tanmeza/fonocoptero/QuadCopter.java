package cl.tanmeza.fonocoptero;

/**
 * Created by Sebastian on 14-12-2016.
 */
public class QuadCopter {

    public float yaw, pitch, roll, //[grados].
            baroElevacion, gpsElevacion; //[m].
    public long time; // [nanosegundos].
    public double longitude, latitude; //[grados].
    public float gpsExactitud, xPos, yPos; //[m].
    public float xVelocidad, yVelocidad; //[m/s].
    public int nSatelites, gpsEstado; //[].

    public static float getMainAngle(float angle){
        while(angle < -180.0f){
            angle += 360.0f;
        }
        while(angle > 180.0f){
            angle -= 360.0f;
        }
        return angle;
    }

}
