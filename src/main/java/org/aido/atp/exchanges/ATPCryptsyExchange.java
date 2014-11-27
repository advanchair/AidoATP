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
package org.aido.atp.exchanges;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.cryptsy.CryptsyExchange;

import org.aido.atp.Application;
import org.aido.atp.ExchangeManager;
import org.aido.atp.polling.PollingTickerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* AidoATP CryptsyExchange class.
*
* @author advanchair
*/

public class ATPCryptsyExchange extends CryptsyExchange {

	private static final String EXCHANGENAME = "cryptsy";
	private static final String TICKERMANAGERCLASS = PollingTickerManager.class.getName();
	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(ATPCryptsyExchange.class);
	private static ExchangeSpecification exchangeSpecification;

	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}

	public static Exchange newInstance() {
		String apiKey = Application.getInstance().getConfig(EXCHANGENAME + "ApiKey");
		String secretKey= Application.getInstance().getConfig(EXCHANGENAME + "SecretKey");

		log.debug("{} API Key: {}",EXCHANGENAME,apiKey);
		log.debug("{} Secret Key: {}",EXCHANGENAME,secretKey);
		
		Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BTCEExchange.class.getName());
		
		ExchangeSpecification exchangeSpecification = exchange.getDefaultExchangeSpecification();
		exchangeSpecification.setApiKey(apiKey);
		exchangeSpecification.setSecretKey(secretKey);
		exchange.applySpecification(exchangeSpecification);
		
		ExchangeManager.getInstance(EXCHANGENAME).setExchangeSpecification(exchangeSpecification);
		ExchangeManager.getInstance(EXCHANGENAME).setTickerManagerClass(TICKERMANAGERCLASS);
		
		log.info("Connecting to {} Exchange",EXCHANGENAME);			
			
		return exchange;
	}

	public static String getExchangeName() {
		return EXCHANGENAME;
	}
}