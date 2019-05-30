package com.highmobility.samples.androidscaffold;

import android.app.Activity;
import android.os.Bundle;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.GetCapabilities;
import com.highmobility.autoapi.GetLockState;
import com.highmobility.autoapi.GetVehicleStatus;
import com.highmobility.autoapi.LockState;
import com.highmobility.autoapi.LockUnlockDoors;
import com.highmobility.autoapi.VehicleStatus;
import com.highmobility.autoapi.value.Location;
import com.highmobility.autoapi.value.Lock;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.BroadcastError;
import com.highmobility.hmkit.error.DownloadAccessCertificateError;
import com.highmobility.hmkit.error.LinkError;
import com.highmobility.hmkit.error.TelematicsError;
import com.highmobility.value.Bytes;

import timber.log.Timber;

import static timber.log.Timber.d;
import static timber.log.Timber.e;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Timber.plant(new Timber.DebugTree()); // enable HMKit logging
        /*
         * Before using HMKit, you'll have to initialise the HMKit singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   HMKit.getInstance().initialise(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         */

        // PASTE THE SNIPPET HERE

        String accessToken = "";

        HMKit.getInstance().downloadAccessCertificate(accessToken, new HMKit.DownloadCallback() {
            @Override
            public void onDownloaded(DeviceSerial serial) {
                d("Certificate downloaded for vehicle: %s", serial);
                // Send command to the car through Telematics, make sure that the emulator is
                // opened for this to work, otherwise "Vehicle asleep" will be returned
                workWithTelematics(serial);

                // Also make the device visible through Bluetooth to the car
                workWithBluetooth();
            }

            @Override
            public void onDownloadFailed(DownloadAccessCertificateError error) {
                e("Could not download a certificate with token: %s", error.getMessage());
            }
        });
    }

    private void workWithTelematics(DeviceSerial serial) {
        HMKit.getInstance().getTelematics().sendCommand(new LockUnlockDoors(Lock.UNLOCKED),
                serial, new Telematics.CommandCallback() {
                    @Override
                    public void onCommandResponse(Bytes bytes) {
                        // Parse command here
                        Command command = CommandResolver.resolve(bytes);

                        if (command instanceof LockState) {
                            // Your code here
                        }
                    }

                    @Override
                    public void onCommandFailed(TelematicsError error) {
                    }
                });

        Command command = new GetLockState();
        HMKit.getInstance().getTelematics().sendCommand(command, serial,
                new Telematics.CommandCallback() {
                    @Override
                    public void onCommandResponse(Bytes bytes) {
                        Command command = CommandResolver.resolve(bytes);

                        if (command instanceof LockState) {
                            LockState state = (LockState) command;
                            d("Telematics GetLockState response: ");
                            d("Front left state: %s",
                                    state.getOutsideLock(Location.FRONT_LEFT).getValue().getLock());
                            d("Front right state: %s", state
                                    .getOutsideLock(Location.FRONT_RIGHT).getValue().getLock());
                            d("Rear right state: %s", state
                                    .getOutsideLock(Location.REAR_RIGHT).getValue().getLock());
                            d("Rear left state: %s", state
                                    .getOutsideLock(Location.REAR_LEFT).getValue().getLock());
                        } else if (command instanceof VehicleStatus) {
                            d("vin: " + ((VehicleStatus) command).getVin().getValue());
                        }
                    }

                    @Override
                    public void onCommandFailed(TelematicsError error) {
                        d("Could not send a command through " +
                                "telematics: " + "" + error.getCode() + " " + error.getMessage());
                    }
                });
    }

    private void workWithBluetooth() {
        // Start Bluetooth broadcasting, so that the car can connect to this device
        final Broadcaster broadcaster = HMKit.getInstance().getBroadcaster();

        if (broadcaster == null) return; // emulator

        broadcaster.setListener(new BroadcasterListener() {
            @Override
            public void onStateChanged(Broadcaster.State state) {
                d("Broadcasting state changed: %s", state);
            }

            @Override
            public void onLinkReceived(ConnectedLink connectedLink) {
                connectedLink.setListener(new ConnectedLinkListener() {
                    @Override
                    public void onAuthorizationRequested(ConnectedLink connectedLink,
                                                         ConnectedLinkListener.AuthorizationCallback authorizationCallback) {
                        // Approving without user input
                        authorizationCallback.approve();
                    }

                    @Override
                    public void onAuthorizationTimeout(ConnectedLink connectedLink) {

                    }

                    @Override
                    public void onStateChanged(final Link link, Link.State state) {
                        if (link.getState() == Link.State.AUTHENTICATED) {
                            Bytes command = new GetCapabilities();
                            link.sendCommand(command, new Link.CommandCallback() {
                                @Override
                                public void onCommandSent() {
                                    d("Command successfully sent through Bluetooth");
                                }

                                @Override
                                public void onCommandFailed(LinkError linkError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCommandReceived(final Link link, Bytes bytes) {
                        final Command command;

                        command = CommandResolver.resolve(bytes);
                        if (command instanceof Capabilities) {

                            link.sendCommand(new GetVehicleStatus(), new
                                    Link.CommandCallback() {
                                        @Override
                                        public void onCommandSent() {
                                            d("VS Command successfully sent through Bluetooth");
                                        }

                                        @Override
                                        public void onCommandFailed(LinkError
                                                                            linkError) {

                                        }
                                    });

                        } else if (command instanceof VehicleStatus) {
                            VehicleStatus status = (VehicleStatus) command;
                            d("BLE Vehicle Status received\nvin:%s", status.getVin());

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
                d("Bluetooth broadcasting started");
            }

            @Override
            public void onBroadcastingFailed(BroadcastError broadcastError) {
                d("Bluetooth broadcasting started: " + broadcastError);
            }
        });
    }
}