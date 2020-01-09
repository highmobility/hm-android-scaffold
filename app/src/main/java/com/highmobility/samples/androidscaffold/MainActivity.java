/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.samples.androidscaffold;

import android.app.Activity;
import android.os.Bundle;

import com.highmobility.autoapi.Capabilities;
import com.highmobility.autoapi.Command;
import com.highmobility.autoapi.CommandResolver;
import com.highmobility.autoapi.Doors;
import com.highmobility.autoapi.VehicleStatus;
import com.highmobility.autoapi.value.Location;
import com.highmobility.autoapi.value.LockState;
import com.highmobility.crypto.value.DeviceSerial;
import com.highmobility.hmkit.Broadcaster;
import com.highmobility.hmkit.BroadcasterListener;
import com.highmobility.hmkit.ConnectedLink;
import com.highmobility.hmkit.ConnectedLinkListener;
import com.highmobility.hmkit.HMKit;
import com.highmobility.hmkit.Link;
import com.highmobility.hmkit.Telematics;
import com.highmobility.hmkit.error.AuthenticationError;
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
        HMKit.getInstance().getTelematics().sendCommand(new Doors.LockUnlockDoors(LockState.UNLOCKED),
                serial, new Telematics.CommandCallback() {
                    @Override
                    public void onCommandResponse(Bytes bytes) {
                        // Parse command here
                        Command command = CommandResolver.resolve(bytes);

                        if (command instanceof Doors.State) {
                            // Your code here
                        }
                    }

                    @Override
                    public void onCommandFailed(TelematicsError error) {
                    }
                });

        Command command = new Doors.GetState();
        HMKit.getInstance().getTelematics().sendCommand(command, serial,
                new Telematics.CommandCallback() {
                    @Override
                    public void onCommandResponse(Bytes bytes) {
                        Command command = CommandResolver.resolve(bytes);

                        if (command instanceof Doors.State) {
                            Doors.State state = (Doors.State) command;
                            d("Telematics GetLockState response: ");
                            d("Front left state: %s",
                                    state.getLock(Location.FRONT_LEFT).getValue().getLockState());
                            d("Front right state: %s", state
                                    .getLock(Location.FRONT_RIGHT).getValue().getLockState());
                            d("Rear right state: %s", state
                                    .getLock(Location.REAR_RIGHT).getValue().getLockState());
                            d("Rear left state: %s", state
                                    .getLock(Location.REAR_LEFT).getValue().getLockState());
                        } else if (command instanceof VehicleStatus.State) {
                            d("vin: " + ((VehicleStatus.State) command).getVin().getValue());
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
                    public void onAuthenticationRequest(ConnectedLink connectedLink,
                                                        ConnectedLinkListener.AuthenticationRequestCallback authorizationCallback) {
                        // Approving without user input
                        authorizationCallback.approve();
                    }

                    @Override
                    public void onAuthenticationRequestTimeout(ConnectedLink connectedLink) {

                    }

                    @Override
                    public void onStateChanged(final Link link, Link.State newsState,
                                               Link.State oldState) {
                        if (link.getState() == Link.State.AUTHENTICATED) {
                            Bytes command = new Capabilities.GetCapabilities();
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
                    public void onAuthenticationFailed(Link link,
                                                       AuthenticationError authenticationError) {

                    }

                    @Override
                    public void onCommandReceived(final Link link, Bytes bytes) {
                        final Command command;

                        command = CommandResolver.resolve(bytes);
                        if (command instanceof Capabilities.State) {

                            link.sendCommand(new VehicleStatus.GetVehicleStatus(), new
                                    Link.CommandCallback() {
                                        @Override
                                        public void onCommandSent() {
                                            d("VS Command successfully sent through Bluetooth");
                                        }

                                        @Override
                                        public void onCommandFailed(LinkError linkError) {

                                        }
                                    });

                        } else if (command instanceof VehicleStatus.State) {
                            VehicleStatus.State status = (VehicleStatus.State) command;
                            d("BLE Vehicle Status received\nvin:%s", status.getVin().getValue());

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