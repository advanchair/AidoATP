/**
 * Copyright (c) 2013 Aido
 * 
 * This file is part of Aido ATP.
 * 
 * Aido ATP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Aido ATP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Aido ATP.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aido.atp.polling;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.aido.atp.Application;
import org.aido.atp.ExchangeManager;
import org.aido.atp.TickerManager;

import si.mazi.rescu.HttpStatusIOException;

import com.xeiam.xchange.currency.Currencies;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.service.polling.PollingMarketDataService;

/**
* Polling Ticker Manager class.
*
* @author Aido, advanchair
*/

public class PollingTickerManager extends TickerManager {

	private String exchangeName;
	private PollingMarketDataService marketData;
	private String currency;

	public PollingTickerManager(String currency, String exchangeName) {
		super(currency,exchangeName);
		this.exchangeName = exchangeName;
		this.currency = currency;

		try {
			marketData = ExchangeManager.getInstance(exchangeName).getExchange().getPollingMarketDataService();
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}

	public void getTick() {
		try {
//			checkTick(marketData.getTicker(Currencies.BTC, currency.getCurrencyCode()));
//TODO implement other tickers
			log.info("PollingTickerManager.getTick() currency: "+currency);
			checkTick(marketData.getTicker(new CurrencyPair("BTC",currency), currency));
			TimeUnit.SECONDS.sleep(Integer.parseInt(Application.getInstance().getConfig("PollingInterval")));
		} catch (com.xeiam.xchange.ExchangeException | HttpStatusIOException e) {
			Socket testSock = null;
			try {
				log.warn("WARNING: Testing connection to {} exchange",exchangeName);
				testSock = new Socket(ExchangeManager.getInstance(exchangeName).getHost(),ExchangeManager.getInstance(exchangeName).getPort());
				if (testSock != null) {
					log.info("connection to {} established",exchangeName);
				}
			}
			catch (java.io.IOException e1) {
				try {
					log.error("ERROR: Cannot connect to {} exchange. Sleeping for one minute",exchangeName);
					TimeUnit.MINUTES.sleep(1);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected {} exception, ticker manager shutting down now!. Details are listed below.",exchangeName);
			log.error(e.getMessage(),exchangeName);
			log.error(e.getLocalizedMessage(),exchangeName);
//			stop();
		}
	}
	
}
