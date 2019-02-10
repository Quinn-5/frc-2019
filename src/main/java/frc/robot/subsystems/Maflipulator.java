package frc.robot.subsystems;

import com.chopshop166.chopshoplib.outputs.SendableSpeedController;

import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.InstantCommand;
import edu.wpi.first.wpilibj.command.PIDCommand;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.interfaces.Potentiometer;
import frc.robot.Robot;
import frc.robot.RobotMap;

public class Maflipulator extends Subsystem {

    public enum MaflipulatorSide {
        kFront, kBack;
    }

    private final static double FRONT_LOWER_ANGLE = 70;
    private final static double FRONT_UPPER_ANGLE = 180;
    private final static double BACK_LOWER_ANGLE = 290;
    private final static double BACK_UPPER_ANGLE = 180;

    private final static double FLIP_MOTOR_SPEED = 1;

    private MaflipulatorSide currentPosition;

    private SendableSpeedController flipMotor;
    private Potentiometer anglePot;

    double angleCorrection;
    PIDController anglePID;

    public Maflipulator(final RobotMap.MaflipulatorMap map) { // NOPMD
        super();
        flipMotor = map.getFlipMotor();
        anglePot = map.getMaflipulatorPot();

        anglePID = new PIDController(.01, .0009, 0.0, 0.0, anglePot, (double value) -> {
            angleCorrection = value;
        });

        if (anglePot.get() < 180)
            currentPosition = MaflipulatorSide.kFront;
        else
            currentPosition = MaflipulatorSide.kBack;

    }

    @Override
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        setDefaultCommand(restrictRotate());
    }

    public Command manualFlip() {
        // The command is named "Manual Flip" and requires this subsystem.
        return new Command("Manual Flip", this) {
            @Override
            protected void initialize() {
                // Called just before this Command runs the first time
            }

            @Override
            protected void execute() {
                flipMotor.set(Robot.xBoxCoPilot.getY(Hand.kRight));
            }

            @Override
            protected boolean isFinished() {
                return false;
            }
        };
    }

    protected double restrict(double flipSpeed) {
        if (currentPosition == MaflipulatorSide.kFront) {
            if (flipSpeed > 0 && anglePot.get() >= FRONT_UPPER_ANGLE) {
                flipSpeed = 0;
            }
            if (flipSpeed < 0 && anglePot.get() <= FRONT_LOWER_ANGLE) {
                flipSpeed = 0;
            }
        } else {
            if (flipSpeed > 0 && anglePot.get() <= BACK_UPPER_ANGLE) {
                flipSpeed = 0;
            }
            if (flipSpeed < 0 && anglePot.get() >= BACK_LOWER_ANGLE) {
                flipSpeed = 0;
            }
        }
        return flipSpeed;
    }

    public Command restrictRotate() {
        // The command is named "Restrict Rotate" and requires this subsystem.
        return new Command("Restrict Rotate", this) {

            @Override
            protected void execute() {
                double flipSpeed = Robot.xBoxCoPilot.getY(Hand.kRight);
                flipSpeed = restrict(flipSpeed);
                flipMotor.set(flipSpeed);
            }

            @Override
            protected boolean isFinished() {
                return false;
            }

        };
    }

    public Command Flip() {
        // The command is named "Flip" and requires this subsystem.
        return new Command("Flip", this) {
            @Override
            protected void initialize() {
                if (currentPosition == MaflipulatorSide.kFront) {
                    flipMotor.set(FLIP_MOTOR_SPEED);
                } else {
                    flipMotor.set(-FLIP_MOTOR_SPEED);
                }
            }

            @Override
            protected boolean isFinished() {
                // Make this return true when this Command no longer needs to run execute()
                if (currentPosition == MaflipulatorSide.kFront && anglePot.get() >= 270) {
                    return true;
                } else if (anglePot.get() <= 90) {
                    return true;
                }
                return false;
            }

            @Override
            protected void end() {
                flipMotor.set(0);
            }
        };
    }

    public Command PIDFlip() {
        return new InstantCommand("PID Flip", this, () -> {
            if (currentPosition == MaflipulatorSide.kFront) {
                if (anglePot.get() < (FRONT_LOWER_ANGLE + 90) / 2) {
                    moveToPosition(FRONT_UPPER_ANGLE);
                } else {
                    moveToPosition(FRONT_LOWER_ANGLE);
                }
            } else {
                if (anglePot.get() > (BACK_LOWER_ANGLE + 270) / 2) {
                    moveToPosition(BACK_UPPER_ANGLE);
                } else {
                    moveToPosition(BACK_LOWER_ANGLE);
                }
            }
        });
    }

    public Command moveToPosition(double targetPosition) {
        return new PIDCommand("Move to Position", .01, .0009, 0.0, this) {

            @Override
            protected void initialize() {
                anglePID.reset();
                anglePID.setSetpoint(targetPosition);
                anglePID.enable();
            }

            @Override
            protected boolean isFinished() {
                return anglePID.onTarget();
            }

            @Override
            protected void end() {
                flipMotor.set(0);
            }

            @Override
            protected double returnPIDInput() {
                return anglePot.pidGet();
            }

            @Override
            protected void usePIDOutput(double output) {
                double flipSpeed = angleCorrection;
                flipSpeed = restrict(flipSpeed);
                flipMotor.set(flipSpeed);
            }
        };
    }
}