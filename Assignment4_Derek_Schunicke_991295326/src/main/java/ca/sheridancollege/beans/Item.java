package ca.sheridancollege.beans;

import java.io.Serializable;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
//creating the item bean and declaring its variables
public class Item implements Serializable{

	private static final long serialVersionUID = -4091178415575578335L;
	int id;
	String item;
	String weight;
	String price;
	int inventory;
	int minQty;
	int maxQty;
	
}
