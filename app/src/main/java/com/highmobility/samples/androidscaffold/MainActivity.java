package com.highmobility.samples.androidscaffold;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandParseException;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.VehicleStatus;
import com.highmobility.autoapi.property.DoorLockProperty;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;

import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.Error.BroadcastError;
import com.highmobility.hmkit.Error.DownloadAccessCertificateError;
import com.highmobility.hmkit.Error.LinkError;
import com.highmobility.hmkit.Error.TelematicsError;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Manager;
import com.highmobility.hmkit.Telematics;
import com.highmobility.utils.Bytes;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Scaffold";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Manager.environment = Manager.Environment.TEST;

        /*
         Before using HMKit, you must initialise it with a snippet from the Developer Center:
         - go to the Developer Center
         - LOGIN
         - choose DEVELOP (in top-left, the (2nd) button with a spanner)
         - choose APPLICATIONS (in the left)
         - look for SANDBOX app
         - click on the "Device Certificates" on the app
         - choose the SANDBOX DEVICE
         - copy the whole snippet
         - paste it below this comment box
         - you made it!

         Bonus steps after completing the above:
         - relax
         - celebrate
         - explore the APIs


         An example of a snippet copied from the Developer Center (do not use, will obviously not work):

            Manager.getInstance().initialize(
                Base64String,
                Base64String,
                Base64String,
                getApplicationContext()
            );

         */

        // PASTE INIT SNIPPET HERE

        // Send command to the car through Telematics, make sure that the emulator is opened for
        // this to work, otherwise "Vehicle asleep" will be returned
        workWithTelematics();

        // Also make the device visible through Bluetooth to the car
        workWithBluetooth();
    }

    private void workWithTelematics() {
        /*
         Before using Telematics in HMKit, you must get the Access Certificate for the car /
         emulator:
         - go to the Developer Center
         - LOGIN
         - go to Tutorials ›› SDK ›› Android for instructions to connect a service to the car
         - authorise the service
         - take a good look into the mirror, you badass
         - open the SANDBOX car emulator
         - on the left, in the Authorised Services list, choose the Service you used before
         - copy the ACCESS TOKEN
         - paste it below to the appropriately named variable

         Bonus steps again:
         - get a beverage
         - quench your thirst
         - change the world with your mind
         - explore the APIs

         An example of an access token:

         awb4oQwMHxomS926XHyqdx1d9nYLYs94GvlJYQCblbP_wt-aBrpNpmSFf2qvhj18GWXXQ
         -aAtSaa4rnwBAHs5wpe1aK-3bD4xfQ3qtOS1QNV3a3iJVg03lTdNOLjFxlIOA

         */
        Manager.getInstance().downloadCertificate("DfuvdzxDkPJPwQPR0bE47rgVzAkcKtqM9oH7IrY6ehLbflCM5OgnQP_dWbh5nD3-6rOTx5qPmJxKzXihaIYeNH8kqL1H3abIkXTTjJKAwD8uWfocYroiFPGMgQwxAuA9CA", new Manager.DownloadCallback() {
                    @Override
                    public void onDownloaded(byte[] serial) {
                        Log.d(TAG, "Certificate downloaded for vehicle: " + Bytes.hexFromBytes
                                (serial));

                        byte[] command = new LockUnlockDoors(DoorLockProperty.LockState.UNLOCKED)
                                .getBytes();

                        Manager.getInstance().getTelematics().sendCommand(command, serial, new
                                Telematics
                                        .CommandCallback() {
                                    @Override
                                    public void onCommandResponse(byte[] bytes) {
                                        try {
                                            Command command = CommandResolver.resolve(bytes);

                                            if (command instanceof LockState) {
                                                LockState state = (LockState) command;

                                                Log.d(TAG, "Front left state: " + state
                                                        .getDoorLockState
                                                                (DoorLockProperty.Location
                                                                        .FRONT_LEFT)
                                                        .getLockState());
                                                Log.d(TAG, "Front right state: " + state
                                                        .getDoorLockState
                                                                (DoorLockProperty.Location
                                                                        .FRONT_RIGHT)
                                                        .getLockState());
                                                Log.d(TAG, "Rear right state: " + state
                                                        .getDoorLockState
                                                                (DoorLockProperty.Location
                                                                        .REAR_RIGHT)
                                                        .getLockState());
                                                Log.d(TAG, "Rear left state: " + state
                                                        .getDoorLockState
                                                                (DoorLockProperty.Location
                                                                        .REAR_LEFT)
                                                        .getLockState());
                                            }
                                        } catch (CommandParseException e) {
                                            Log.e(TAG, e.getLocalizedMessage());
                                        }
                                    }

                                    @Override
                                    public void onCommandFailed(TelematicsError error) {
                                        Log.d(TAG, "Could not send a command through telematics: " +
                                                "" + error.getCode() + " " + error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onDownloadFailed(DownloadAccessCertificateError error) {
                        Log.d(TAG, "Could not download a certificate with token: " + error
                                .getMessage());
                    }
                });
    }

    private void workWithBluetooth() {
        // Start Bluetooth broadcasting, so that the car can connect to this device
        final Broadcaster broadcaster = Manager.getInstance().getBroadcaster();

        broadcaster.setListener(new BroadcasterListener() {
            @Override
            public void onStateChanged(Broadcaster.State state) {
                Log.d(TAG, "Broadcasting state changed: " + state);
            }

            @Override
            public void onLinkReceived(ConnectedLink connectedLink) {
                connectedLink.setListener(new ConnectedLinkListener() {
                    @Override
                    public void onAuthorizationRequested(ConnectedLink connectedLink,
                                                         ConnectedLinkListener
                                                                 .AuthorizationCallback
                                                                 authorizationCallback) {
                        // Approving without user input
                        authorizationCallback.approve();
                    }

                    @Override
                    public void onAuthorizationTimeout(ConnectedLink connectedLink) {

                    }

                    @Override
                    public void onStateChanged(final Link link, Link.State state) {
                        if (link.getState() == Link.State.AUTHENTICATED) {
                            byte[] command = new GetCapabilities().getBytes();
                            link.sendCommand(command, new Link.CommandCallback() {
                                @Override
                                public void onCommandSent() {
                                    Log.d(TAG, "Command successfully sent through " +
                                            "Bluetooth");
                                }

                                @Override
                                public void onCommandFailed(LinkError linkError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCommandReceived(final Link link, byte[] bytes) {
                        try {
                            final Command command = CommandResolver.resolve(bytes);
                            if (command instanceof Capabilities) {
                                link.sendCommand(new GetVehicleStatus().getBytes(), new
                                        Link.CommandCallback() {
                                            @Override
                                            public void onCommandSent() {
                                                Log.d(TAG, "VS Command successfully " +
                                                        "sent through " +
                                                        "Bluetooth");
                                            }

                                            @Override
                                            public void onCommandFailed(LinkError
                                                                                linkError) {

                                            }
                                        });

                            } else if (command instanceof VehicleStatus) {
                                Log.d("hm", "onCommandReceived: Vehicle Status");
                            }

                        } catch (CommandParseException e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    }

                });
            }

            @Override
            public void onLinkLost(ConnectedLink connectedLink) {
                // Bluetooth disconnected
            }
        });

        broadcaster.startBroadcasting(new Broadcaster.StartCallback() {
            @Override
            public void onBroadcastingStarted() {
                Log.d(TAG, "Bluetooth broadcasting started");
            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                Log.d(TAG, "Bluetooth broadcasting started: " + broadcastError);
            }
        });
    }
}