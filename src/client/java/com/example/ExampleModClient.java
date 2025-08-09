package com.example;

import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client-specific registrations are performed from dedicated classes
        ClientInitBootstrap.initialize();
    }
}