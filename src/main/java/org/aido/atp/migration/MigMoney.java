package org.aido.atp.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
* This class is used to replace BigMoney.
*
* @author advanchair
*/

public class MigMoney
{
	BigDecimal amount = new BigDecimal(0);
	String currencyUnit;
	
	public MigMoney(BigDecimal amount, String currencyUnit)
	{
		this.amount = amount;
		this.currencyUnit = currencyUnit;
	}
	
	public static MigMoney zero(String currencyUnit)
	{
		return new MigMoney(new BigDecimal(0), currencyUnit);
	}
	
	public static MigMoney of(String currencyUnit, BigDecimal amount)
	{
		return new MigMoney(amount, currencyUnit);
	}
	
	public String getCurrencyUnit()
	{
		return currencyUnit;
	}

	public BigDecimal getAmount()
	{
		return amount;
	}

	public boolean isPositive()
	{
		boolean ret = false;
		
		if(amount.doubleValue() > 0)
			ret = true;
		
		return ret;
	}

	public MigMoney convertedTo(String currencyUnit, BigDecimal conversionMultipler)
	{
		return new MigMoney(getAmount().multiply(conversionMultipler), currencyUnit);
	}

	public MigMoney plus(MigMoney plus)
	{
		this.amount.add(plus.getAmount());

		return this;
	}

	public MigMoney minus(MigMoney minus)
	{
		this.amount.subtract(minus.getAmount());

		return this;
	}

	public MigMoney withScale(int scale, RoundingMode roundingMode)
	{
		amount.setScale(scale, roundingMode);

		return this;
	}

	public boolean isGreaterThan(MigMoney other)
	{
		boolean ret = false;
		
		if(amount.doubleValue() > other.getAmount().doubleValue())
			ret = true;
		
		return ret;
	}

	public boolean isLessThan(MigMoney other)
	{
		boolean ret = false;
		
		if(amount.doubleValue() < other.getAmount().doubleValue())
			ret = true;
		
		return ret;
	}

	public MigMoney multipliedBy(BigDecimal multiplicant)
	{
		amount.multiply(multiplicant);

		return this;
	}
	
	public MigMoney multipliedBy(Double multiplicant)
	{
		amount.multiply(new BigDecimal(multiplicant));

		return this;
	}

	public MigMoney dividedBy(BigDecimal divisor, RoundingMode roundingMode)
	{
		amount.divide(divisor, roundingMode);
		
		return this;
	}

	public MigMoney dividedBy(Long divisor, RoundingMode roundingMode)
	{
		amount.divide(new BigDecimal(divisor), roundingMode);
		
		return this;
	}

	public boolean isZero()
	{
		boolean ret = false;
		
		if(amount.compareTo(BigDecimal.ZERO) == 0)
			ret = true;
		
		return ret;
	}

	public int compareTo(MigMoney other)
	{
		return amount.compareTo(other.getAmount());
	}
}
