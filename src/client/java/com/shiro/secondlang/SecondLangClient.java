package com.shiro.secondlang;

import net.fabricmc.api.ClientModInitializer;

public class SecondLangClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SecondLangConfig.load();
	}
}
