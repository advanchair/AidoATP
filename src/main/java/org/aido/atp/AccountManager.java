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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.aido.atp.migration.MigMoney;

import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.service.polling.PollingAccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Account manager class.
*
* @author Aido
*/

public class AccountManager implements Runnable {

	private static HashMap<String, AccountManager> instances = new HashMap<String, AccountManager>();
	private String exchangeName;
	private AccountInfo accountInfo;
	private HashMap<String, ArrayList<MigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private static Logger log;
	private PollingAccountService accountService;
	private List<Wallet> wallets;

	public static AccountManager getInstance(String exchangeName) {
		if(!instances.containsKey(exchangeName))
			instances.put(exchangeName, new AccountManager(exchangeName));
		return instances.get(exchangeName);
	}

	private AccountManager(String exchangeName){
		this.exchangeName = exchangeName;
		try {
			log = LoggerFactory.getLogger(AccountManager.class);
			books = new HashMap<String, ArrayList<MigMoney>>();

			// Interested in the private account functionality (authentication)
			accountService = ExchangeManager.getInstance(exchangeName).getExchange().getPollingAccountService();

			// Get the account information
			accountInfo = accountService.getAccountInfo();
			log.info("{} AccountInfo as String: {}",exchangeName,accountInfo.toString());
			refreshAccounts();
			startTickers();
		} catch (com.xeiam.xchange.ExchangeException | si.mazi.rescu.HttpStatusIOException e) {
			Socket testSock = null;
			while (true) {
				try {
					log.warn("WARNING: Testing connection to exchange {}",exchangeName);
					testSock = new Socket(ExchangeManager.getInstance(exchangeName).getHost(),ExchangeManager.getInstance(exchangeName).getPort());
					if (testSock != null) { break; }
				}
				catch (java.io.IOException e1) {
					try {
						log.error("ERROR: Cannot connect to exchange {}. Sleeping for one minute",exchangeName);
						TimeUnit.MINUTES.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected {} exception, shutting down now!.Details are listed below.",exchangeName);
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public synchronized void run() {
		refreshAccounts();
	}

	public MigMoney getBalance(String currency) throws WalletNotFoundException{
		refreshAccounts();
		wallets = accountInfo.getWallets();

		for(Wallet wallet : wallets)
		{
			if(wallet.getCurrency().equals(currency)){
				return new MigMoney(wallet.getBalance(), currency);
			}
		}
		log.error("ERROR: Could not find a {} wallet for the currency {}. Exiting now!",exchangeName,currency);
		throw new WalletNotFoundException();
	}

	public synchronized void refreshAccounts()
	{
		try {
			accountInfo = accountService.getAccountInfo();
			updateBooks();
			ProfitLossAgent.getInstance().updateBalances(accountInfo.getWallets());
		}
		catch (Exception e)
		{
			log.error(e.getMessage());
		}
	}

	private void updateBooks() {
		wallets = accountInfo.getWallets();
		for(Wallet wallet : wallets){
			String currency = wallet.getCurrency();

			//Do we have a new currency in our wallet?
			if(!books.containsKey(currency)){
				//Make some space for it.
				books.put(currency,new ArrayList<MigMoney>());
			}

			ArrayList<MigMoney> ledger = books.get(currency);
			ledger.add(new MigMoney(wallet.getBalance(),currency));
//			ledger.add(wallet.getBalance());
		}
	}

	public AccountInfo getAccountInfo() {
		return accountInfo;
	}

	public void startTickers() {
		ThreadGroup tickerThreadGroup = new ThreadGroup("Tickers");
		for(Wallet wallet : wallets) {
			String currency = wallet.getCurrency();
			
			if(!currency.equals("BTC") && !currency.equals("NMC") && !currency.equals("LTC") && !currency.equals("PPC") && !currency.equals("TRC") && !currency.equals("NVC") && !currency.equals("FTC") && !currency.equals("CNC") ) {				
				if (Application.getInstance().getConfig("UseLocalOnly").equals("1")) {
					if (currency.equals(Application.getInstance().getConfig("LocalCurrency"))) {
						Thread tickermanagerManagerThread = new Thread(tickerThreadGroup,TickerManager.getInstance(exchangeName,currency));
						tickermanagerManagerThread.start();
					}
				} else {
					Thread tickermanagerManagerThread = new Thread(tickerThreadGroup,TickerManager.getInstance(exchangeName,currency));
					tickermanagerManagerThread.start();
				}
			}			
		}
	}
}
