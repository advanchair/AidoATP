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
package org.aido.atp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.aido.atp.migration.MigMoney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Trend Observer class.
*
* @author Aido, advanchair
*/

public class TrendObserver implements Runnable {

	private MigMoney vwap;
	private MigMoney shortSMA;
	private MigMoney longSMA;
	private MigMoney shortEMA;
	private MigMoney longEMA;
	private MigMoney shortMACD;
	private MigMoney longMACD;
	private MigMoney sigLineMACD;
	private ATPTicker high;
	private ATPTicker low;
	private ATPTicker last;
	private ArrayList<ATPTicker> ticker;
	private Integer bidArrow;
	private Integer askArrow;
	private Integer trendArrow;
	private Integer tickerSize;
	private Logger log;
	private boolean learningComplete;
	private String localCurrency;
	private String exchangeName;
	
	public TrendObserver(String exchangeName, ArrayList<ATPTicker> marketData) {
		log = LoggerFactory.getLogger(TrendObserver.class);
		this.ticker = marketData;
		this.exchangeName = exchangeName;
		localCurrency = ticker.get(0).getLast().getCurrencyUnit();
		learningComplete = false;
		if(ticker != null && !ticker.isEmpty()) {
			if (ticker.size() < Integer.parseInt(Application.getInstance().getConfig("MinTickSize"))){
				log.info(exchangeName + " " + localCurrency+" Ticker size: "+ticker.size()+". Trend observer does not currently have enough "+localCurrency+" data to determine trend.");
				learningComplete = false;
			} else {
				learningComplete = true;
			}
		} else {
			log.info(exchangeName + "Trend observer currently has no "+localCurrency+" ticker data");
			learningComplete = false;
		}	
	}
	
	@Override
	public void run() {
		//(Re)initialize variables
		trendArrow = 0;
		bidArrow = 0;
		askArrow = 0;
		shortSMA = MigMoney.zero(localCurrency);
		longSMA = MigMoney.zero(localCurrency);
		shortEMA = MigMoney.zero(localCurrency);
		longEMA = MigMoney.zero(localCurrency);
		shortMACD = MigMoney.zero(localCurrency);
		longMACD = MigMoney.zero(localCurrency);
		sigLineMACD = MigMoney.zero(localCurrency);
		
		int idx = 0;
		Integer shortMASize = Integer.valueOf(Application.getInstance().getConfig("ShortMATickSize"));
		Integer shortMACDSize = Integer.valueOf(Application.getInstance().getConfig("ShortMACDTickSize"));
		Integer longMACDSize = Integer.valueOf(Application.getInstance().getConfig("LongMACDTickSize"));
		Integer sigLineMACDSize = Integer.valueOf(Application.getInstance().getConfig("SigLineMACDSize"));
		Double expShortEMA = Double.valueOf(0);
		Double expLongEMA = Double.valueOf(0);
		Double expShortMACD = Double.valueOf(0);
		Double expLongMACD = Double.valueOf(0);
		Double expSigLineMACD = Double.valueOf(0);
		MigMoney sumShortSMA = MigMoney.zero(localCurrency);
		MigMoney sumLongSMA = MigMoney.zero(localCurrency);	

		//Items in here are done once for every item in the ticker
		MigMoney newBid = null, oldBid = MigMoney.zero(localCurrency);
		MigMoney newAsk = null, oldAsk = MigMoney.zero(localCurrency);
		MigMoney newPrice = null, oldPrice = MigMoney.zero(localCurrency);
		BigDecimal newVolume = null, oldVolume = BigDecimal.ZERO;
		BigDecimal totalVolume = BigDecimal.ZERO, absVolume = null, changedVolume = null;
		
		//VWAP = Volume Weighted Average Price
		//Each (transaction price * transaction volume) / total volume
		//We are concerned not only with current vwap, but previous vwap.
		// This is because the differential between the two is an important market indicator
		
		vwap = MigMoney.zero(localCurrency);
		
		synchronized(ticker) {
			
			//ticker - could be empty if there is no new data in over an hour, we've been disconnected, or the TickerManager thread has crashed.
			if(!ticker.isEmpty()) {
				tickerSize = ticker.size();
				low = ticker.get(0);
				high = ticker.get(0);
				last = ticker.get(tickerSize - 1);
				
				if (shortMASize > tickerSize) {
					shortMASize = tickerSize;
				}
				shortEMA = ticker.get(tickerSize - shortMASize).getLast();

				if (shortMACDSize > tickerSize) {
					shortMACDSize = tickerSize;
				}
				shortMACD = ticker.get(tickerSize - shortMACDSize).getLast();

				if (longMACDSize > tickerSize) {
					longMACDSize = tickerSize;
				}
				longMACD = ticker.get(tickerSize - longMACDSize).getLast();
				sigLineMACD = shortMACD.minus(longMACD);

				longEMA = ticker.get(0).getLast();
				expShortEMA = (double) 2 / (shortMASize + 1);
				expLongEMA = (double) 2 / (tickerSize + 1);
				expShortMACD = (double) 2 / (shortMACDSize + 1);
				expLongMACD = (double) 2 / (longMACDSize + 1);
				expSigLineMACD = (double) 2 / (sigLineMACDSize + 1);
			}

			for(ATPTicker tick : ticker){		
				
				//The first thing we want to look at is the volume
				//We need a changed volume
				//Changed volume is new volume - old volume
				//We need 2 volumes, a total volume & an absolute volume
				
				//The volume of this tick, by itself
				newVolume = new BigDecimal(new BigInteger(""+tick.getVolume()));
				changedVolume = newVolume.subtract(oldVolume);
				absVolume = changedVolume.abs();
				
				newPrice = tick.getLast();
				newBid = tick.getBid();
				newAsk = tick.getAsk();
		
				if(newPrice.isGreaterThan(high.getLast())){					
					high = tick;
				}else if(newPrice.isLessThan(low.getLast())){
					low = tick;
				}
				
				if(newPrice.isGreaterThan(oldPrice)){
					trendArrow++;
				}else if(newPrice.isLessThan(oldPrice)){
					trendArrow--;
				}
				
				if(newBid.isGreaterThan(oldBid)){
					bidArrow++;
				}else if(newBid.isLessThan(oldBid)){
					bidArrow--;
				}
				
				if(newAsk.isGreaterThan(oldAsk)){
					askArrow++;
				}else if(newAsk.isLessThan(oldAsk)){
					askArrow--;
				}
				
				vwap = vwap.plus(newPrice.multipliedBy(absVolume));
				totalVolume = totalVolume.add(absVolume);
				
				oldVolume = newVolume;
				oldPrice = newPrice;
				oldBid = newBid;
				oldAsk = newAsk;

				if ( idx >= tickerSize - shortMASize ) {
					sumShortSMA = sumShortSMA.plus(newPrice);
					shortEMA = newPrice.multipliedBy(expShortEMA).plus(shortEMA.multipliedBy(1 - expShortEMA));
				}
				
				if ( idx >= tickerSize - shortMACDSize ) {
					shortMACD = newPrice.multipliedBy(expShortMACD).plus(shortMACD.multipliedBy(1 - expShortMACD));
				}

				if ( idx >= tickerSize - longMACDSize ) {
					longMACD = newPrice.multipliedBy(expLongMACD).plus(longMACD.multipliedBy(1 - expLongMACD));
				}
				
				sigLineMACD = shortMACD.minus(longMACD).multipliedBy(expSigLineMACD).plus(sigLineMACD.multipliedBy(1 - expSigLineMACD));

				sumLongSMA = sumLongSMA.plus(newPrice);
				longEMA = newPrice.multipliedBy(expLongEMA).plus(longEMA.multipliedBy(1 - expLongEMA));

				idx++;
			}
			vwap = vwap.dividedBy(totalVolume, RoundingMode.HALF_EVEN);
			shortSMA = sumShortSMA.dividedBy(Long.valueOf(shortMASize),RoundingMode.HALF_EVEN);
			longSMA = sumLongSMA.dividedBy(Long.valueOf(tickerSize),RoundingMode.HALF_EVEN);
		}
		
		log.info(exchangeName + " High "+localCurrency+" :- "+high.toString());
		log.info(exchangeName + " Low "+localCurrency+" :- "+low.toString());			
		log.info(exchangeName + " Current "+localCurrency+" :- "+ticker.get(tickerSize - 1).toString());
		
		if(learningComplete) {
			log.debug("Starting "+exchangeName+" "+localCurrency+" trend trading agent.");
			new Thread(new TrendTradingAgent(this,exchangeName)).start();
		}
	}

	public Integer getTrendArrow() {
		return trendArrow;
	}
	
	public Integer getBidArrow() {
		return bidArrow;
	}
	
	public Integer getAskArrow() {
		return askArrow;
	}
	
	public MigMoney getVwap() {
		return vwap;
	}
	
	public MigMoney getShortSMA() {
		return shortSMA;
	}
	
	public MigMoney getLongSMA() {
		return longSMA;
	}
	
	public MigMoney getShortEMA() {
		return shortEMA;
	}
	
	public MigMoney getLongEMA() {
		return longEMA;
	}

	public MigMoney getShortMACD() {
		return shortMACD;
	}
	
	public MigMoney getLongMACD() {
		return longMACD;
	}

	public MigMoney getSigLineMACD() {
		return sigLineMACD;
	}

	public ATPTicker getLastTick() {
		return last;
	}
	
	public int getTickerSize() {
		return tickerSize;
	}
	
	public String getCurrency() {
		return localCurrency;
	}
}