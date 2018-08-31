/*	Solarys Chat - Local network UDP broadcaster chat
 * 	Copyright 2018 Yeshe Santos García <civyshk@gmail.com>
 *	
 *	This file is part of Solarys Chat
 *	
 *	Solarys Chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.project104.chat.solaris.gui;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Colors {

	public static Color[] colors = new Color[] {
			new Color(84,179,30),
			new Color(79,63,156),
			new Color(219,147,0),
			new Color(52,138,235),
			new Color(227,41,125),
			new Color(0,146,106),
			new Color(221,78,46),
			new Color(184,99,181),
			new Color(132,145,61),
			new Color(132,82,0)
	};
	
	public static HashMap<Color, Integer> availableColors = new HashMap<Color, Integer>(colors.length);
	
	static {
		for(int i=0; i<colors.length; i++) {
			availableColors.put(colors[i], Integer.valueOf(0));
		}
	}
	
	public static Color getUniqueColor() {
		int i = 0;
		while(true) {
			try {
				final int timesUsed = i;
				Color color = availableColors
					.keySet()
					.stream()
					.filter(c -> availableColors.get(c) == timesUsed)
					.collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
					      Collections.shuffle(collected);
					      return collected;
					  }))
					.get(0);
				availableColors.put(color, timesUsed + 1);
				return color;
			}catch(IndexOutOfBoundsException e) {
				i++;
			}
		}
	}
	
	public static void unuseColor(Color c) {
		int used = availableColors.get(c);
		if(used > 0) {
			used--;
		}else {
			used = 0;
		}
		availableColors.put(c, used);
	}
}
