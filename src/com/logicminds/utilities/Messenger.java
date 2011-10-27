package com.logicminds.utilities;


import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;


public class Messenger {
	String host = "";
		
	public Messenger(String smtphost){
		this.host = smtphost;
		
	}
	
	public void sendmsg(String to, String from, String subj, String body){
	
		
		// Create properties, get Session
		Properties props = new Properties();
		// If using static Transport.send(),
        // need to specify which host to send it to
        props.put("mail.smtp.host", this.host);
        // To see what is going on behind the scene
        props.put("mail.debug", "true");
        Session session = Session.getInstance(props);
        try {
            // Instantiate a message
            Message msg = new MimeMessage(session);

            //Set message attributes
            msg.setFrom(new InternetAddress(from));
            InternetAddress[] address = {new InternetAddress(to)};
            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(subj);
            msg.setSentDate(new Date());

            // Set message content
            msg.setText(body);

            //Send the message
            Transport.send(msg);
        }
        catch (MessagingException mex) {
            // Prints all nested (chained) exceptions as well
            mex.printStackTrace();
        }
        
	}

}//End of class
