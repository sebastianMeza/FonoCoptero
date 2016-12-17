package cl.tanmeza.fonocoptero;

import ioio.lib.spi.Log;

public class PIDReguladorAltura {
    public PIDReguladorAltura(float kp, float ki, float kd, float smoothingStrength, float aPriori){
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.smoothingStrength = smoothingStrength;
        previousDifference = 0.0f;

        integrator = 0.0f;
        differencesMean = 0.0f;
    }

    public float getInput(float targetAngle, float currentAngle, float dt){
        float rawDifference = targetAngle - currentAngle;
        float difference = QuadCopter.getMainAngle(rawDifference);

        // Now, the PID computation can be done.
        float input = 0.0f;


        // Proportional part.
        input += difference * kp;

        // Integral part.
        integrator += difference * ki * dt;
        input += integrator;

        //if(differenceJump)
        //Log.v("Fonocoptero PID", "input pre = "+input);

        // Derivative part, with filtering.
        differencesMean = differencesMean * smoothingStrength + difference * (1 - smoothingStrength);
        float derivative = (differencesMean - previousDifference) / dt;
        previousDifference = differencesMean;
        input += derivative * kd;

        if(Math.abs(difference)<0.1f) {
            input = 0.0f;
        }
        else if(Math.abs(difference)<0.3f) {
            input = input*6;
        }
        else if(Math.abs(difference)<0.6f) {
            input = input*15;
        }
        else{
            input = input*15;
        }
        //Log.v("Fonocoptero PID", "input out= "+input);
        return input;
    }

    public void setCoefficients(float kp, float ki, float kd){
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public void resetIntegrator(){
        integrator = 0.0f;
    }

    private float kp, ki, kd, integrator, smoothingStrength, differencesMean, previousDifference;
}
