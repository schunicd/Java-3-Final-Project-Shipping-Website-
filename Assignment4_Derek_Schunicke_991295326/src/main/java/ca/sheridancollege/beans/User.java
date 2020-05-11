package ca.sheridancollege.beans;
import java.io.Serializable;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
//creating user bean and declaring its variables
public class User implements Serializable{

	private static final long serialVersionUID = 3572409492123030257L;
	private long userId;
	private String userName;
	private String encryptedPassword;
	
}
