//+----------------------------------------------------------------------------+
//|  Copyright Notice:                                                         |
//|                                                                            |
//|      Copyright (C) 1990-2016, International Business Machines              |
//|      Corporation and others. All rights reserved                           |
//+----------------------------------------------------------------------------+
package com.otm.webservice;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.otm.db.Dao;
import com.otm.db.DaoFactory;
import com.otm.db.TU;
import com.otm.log.ServiceLogger;
import com.otm.util.SyncParameters;
import com.otm.util.TmxSaxParser;

// this class should be thread safe
// it's a single instance

public class OtmTMServiceSynchronizes {
	
	// an immutable object
	private final Dao mDao = DaoFactory.getInstance();
	//private final Object mLockObj = new Object();
    //private volatile boolean mDbExisted = false;
	private static Logger mLogger = Logger.getLogger(ServiceLogger.class.getName());
	
	/**
	 * 
	 * @param paramHash
	 * @return
	 */
	public HashMap<String, String> create(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();

		try {
			String memoryname = paramHash.get(SyncParameters.DATASOURCENAME);
			String user = paramHash.get(SyncParameters.USER_ID);
			String password = paramHash.get(SyncParameters.PASSWORD);
            String userList = paramHash.get(SyncParameters.USERIDLIST);
            
			result.put("method",  paramHash.get(SyncParameters.METHOD));
			result.put("name", memoryname);

            // synchronize blocking operation
			// TODO: use configure to createDB in main
/*			synchronized(mLockObj) {
				if(!mDbExisted) {
					try {
						mDao.createDB(serverIP, port,user, password,
								driverClass);
					} catch (PropertyVetoException e) {
						e.printStackTrace();
						result.put("error", e.getMessage());
						return result;
					}
					mDbExisted = true;
					System.out.println(mDao.getDbName()+" create successfully");
				}
			}
*/

           
			try {
				mDao.createMemory(memoryname, user,password,userList);
			} catch (PropertyVetoException e) {
				e.printStackTrace();
				result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
				return result;
			}

		} catch (SQLException e) {
			result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			return result;
		}

		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
	}
	
	
	/**
	 * 
	 * @param paramHash
	 * @return
	 */
	public HashMap<String, String> delete(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		String userid = paramHash.get(SyncParameters.USER_ID);
		String memoryName = paramHash.get(SyncParameters.DATASOURCENAME);
		String user = paramHash.get(SyncParameters.USER_ID);
		
		result.put("method",  paramHash.get(SyncParameters.METHOD));
		result.put("name", memoryName);

		try {
			// no user accessed memory, returned directly
			String memories = mDao.listMemories(user);
			if(memories==null || memories.indexOf(memoryName)==-1 ) {
				result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
//System.out.println(result);
				mLogger.error(result);
				return result;
			}	
						
			String creatorName = mDao.getCreatorName(memoryName);
//System.out.println(creatorName);
			mLogger.info(creatorName);
			if(creatorName!=null && creatorName.equals(userid)) {
//System.out.println(creatorName);
				mDao.deleteMemory(memoryName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			result.put(SyncParameters.RESPONSESTATUSMSG,e.getMessage());
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
	}
	
	
	/**
	 * 
	 * @param paramHash
	 * @return
	 */
	public HashMap<String, String> listUser(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String memoryName = paramHash.get(SyncParameters.DATASOURCENAME);
		String user = paramHash.get(SyncParameters.USER_ID);
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("name", memoryName);
		
		try {
			// no user accessed memory, returned directly
			String memories = mDao.listMemories(user);
			if(memories==null || memories.indexOf(memoryName)==-1 ) {
				result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
//System.out.println(result);
                mLogger.error(result);
				return result;
			}	
			
			String users = mDao.getUserList(memoryName);
			result.put(SyncParameters.USERIDLIST,users);
			
		} catch (SQLException e) {
		    result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			e.printStackTrace();
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
		
	}

	
	/**
	 * 
	 * @param paramHash
	 * @return
	 */
	public HashMap<String, String> addUser(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String memoryName = paramHash.get(SyncParameters.DATASOURCENAME);
		String userToAdd = paramHash.get(SyncParameters.USERTOADD);
		String user = paramHash.get(SyncParameters.USER_ID);
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("name", memoryName);

		try {
			
			// no user accessed memory, returned directly
			String memories = mDao.listMemories(user);
			if(memories==null || memories.indexOf(memoryName)==-1 ) {
				result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
//System.out.println(result);
                mLogger.error(result);
				return result;
			}	
			
			String res = mDao.addUser(memoryName, userToAdd);
			if(res != null) {
				result.put(SyncParameters.RESPONSESTATUSMSG, res);
				return result;
			}
		} catch (SQLException e) {
		    result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			e.printStackTrace();
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
		
	}
	
	
	/**
	 * 
	 * @param paramHash
	 * @return
	 */
	public HashMap<String, String> removeUser(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String memoryName = paramHash.get(SyncParameters.DATASOURCENAME);
		String userToRemove = paramHash.get(SyncParameters.USERTOREMOVE);
		String user = paramHash.get(SyncParameters.USER_ID);
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("name", memoryName);
		
		try {
			// no user accessed memory, returned directly
			String memories = mDao.listMemories(user);
			if(memories==null || memories.indexOf(memoryName)==-1 ) {
				result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
//System.out.println(result);
				mLogger.error(result);
				return result;
			}	
						
			String res = mDao.removeUser(memoryName, userToRemove);
			if(res != null) {
				result.put(SyncParameters.RESPONSESTATUSMSG, res);
				return result;
			}
		} catch (SQLException e) {
		    result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			e.printStackTrace();
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
		
	}
	
	
	public HashMap<String, String> listMemories(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String userid = paramHash.get(SyncParameters.USER_ID);
		
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("userid", userid);
  
		try {
			String allMems = mDao.listMemories(userid);
			if(allMems !=null && !allMems.isEmpty()) {
				result.put(SyncParameters.MEMORYLIST, allMems);
			} else {
				result.put(SyncParameters.RESPONSESTATUSMSG, "There is no memory that "+ userid+" could accessed");
				mLogger.error(result);
				return result;
			}
		} catch (SQLException e) {
		    result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			e.printStackTrace();
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
		return result;
		
	}

	
	public HashMap<String, String> upload(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String memoryname = paramHash.get(SyncParameters.DATASOURCENAME);
		String contents = paramHash.get("tmx-document");
		String user = paramHash.get(SyncParameters.USER_ID);
		
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("name", memoryname);
        
		// call 
		if(contents.isEmpty()) {
			result.put(SyncParameters.RESPONSESTATUSMSG, "no contents passed");
		} else {
			
			try {
				// no user accessed memory, returned directly
				String memories = mDao.listMemories(user);
				if(memories==null || memories.indexOf(memoryname)==-1 ) {
					result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
					mLogger.error(result);
//System.out.println(result);
					return result;
				}	
				
				TmxSaxParser tmx = new TmxSaxParser();
			    List<TU> tus = tmx.parseTmxForTU(contents,false);
                mDao.upload(user, memoryname, tus);
			    result.put(SyncParameters.RESPONSESTATUSMSG, "success");
			} catch ( SQLException | ParserConfigurationException |SAXException |IOException e) {
				e.printStackTrace();
			}
		}
//System.out.println(result);	
		mLogger.info(result);
		return result;
	}
	
	public HashMap<String, String> download(HashMap<String, String> paramHash) {
		HashMap<String, String> result = new HashMap<String, String>();
		
		String userid = paramHash.get(SyncParameters.USER_ID);
		String memoryName = paramHash.get(SyncParameters.DATASOURCENAME);
		String updateCounter = paramHash.get(SyncParameters.UPDATECOUNTER);
		
		result.put("method", paramHash.get(SyncParameters.METHOD));
		result.put("userid", userid);
		result.put("name", memoryName);
		
		
		try {
			// no user accessed memory, returned directly
			String memories = mDao.listMemories(userid);
			if(memories==null || memories.indexOf(memoryName)==-1 ) {
				result.put(SyncParameters.RESPONSESTATUSMSG, "no such memory existed or user can't access to it");
//System.out.println(result);		
				mLogger.error(result);
				return result;
			}	
			
			Map<String,String> tmxStr = mDao.download(userid, memoryName,Long.valueOf(updateCounter));
			if(!tmxStr.isEmpty()) {

				result.put(SyncParameters.TMXDOCUMENT, tmxStr.get(SyncParameters.TMXDOCUMENT));
				result.put(SyncParameters.UPDATECOUNTER, tmxStr.get(SyncParameters.UPDATECOUNTER));
			} else {
				result.put(SyncParameters.RESPONSESTATUSMSG, "No segments downloaded");
				mLogger.info("No segments downloaded");
				return result;
			}
		} catch (SQLException e) {
		    result.put(SyncParameters.RESPONSESTATUSMSG, e.getMessage());
			e.printStackTrace();
			return result;
		}
		
		result.put(SyncParameters.RESPONSESTATUSMSG, "success");
//System.out.println(result);
		mLogger.debug(result);
		return result;
		
	}
}
