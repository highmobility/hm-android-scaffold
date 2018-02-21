package com.highmobility.samples.androidscaffold;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandParseException;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LockState;
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

public class MainActivity extends Activity {

    private static final String TAG = "Scaffold";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Manager.environment = Manager.Environment.TEST;

        /*
         * Before using HMKit, you'll have to initialise the Manager singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   Manager.getInstance().initialize(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         */

        // PASTE THE SNIPPET HERE

        // Send command to the car through Telematics, make sure that the emulator is opened for
        // this to work, otherwise "Vehicle asleep" will be returned
        workWithTelematics();

        // Also make the device visible through Bluetooth to the car
        workWithBluetooth();
    }

    private void workWithTelematics() {
        String accessToken = ""; // PASTE ACCESS TOKEN HERE
        Manager.getInstance().downloadCertificate(accessToken, new
                Manager.DownloadCallback() {
                    @Override
                    public void onDownloaded(byte[] serial) {
                        Log.d(TAG, "Certificate downloaded for vehicle: " + Bytes.hexFromBytes
                                (serial));

                        byte[] command = new GetVehicleStatus().getBytes();
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
                                            } else if (command instanceof VehicleStatus) {
                                                Log.d(TAG, "vin: " + ((VehicleStatus) command)
                                                        .getVin());
                                            }
                                        } catch (CommandParseException e) {
                                            e.printStackTrace();
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
                        final Command command;
                        try {
                            command = CommandResolver.resolve(bytes);
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
                            e.printStackTrace();
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