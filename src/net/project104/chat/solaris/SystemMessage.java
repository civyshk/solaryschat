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

package net.project104.chat.solaris;

public class SystemMessage extends Message {
/* dos tipos de mensajes de sistema:
 * Los que aparecen en 1 chat: tal usuario se ha unido
 * Los que aparecen en los chats en los que participe un usuario: pepito se ha cambiado de nombre
 * Al recibir un tipo 1: se manda al chat correspondiente
 * Al recibir un tipo 2: se manda a todos donde esté ese usuario
 * Al abrir un chat, cargar:
 * 	T1: No hace falta
 *  T2: No hace falta
 * Los SystemMessage se muestran una vez, cuando se mandan, y no vuelven a usarse. No hay que guardarlos
 * en ningún lado
 */
	
	public SystemMessage(String content, long timestamp) {
		super(content, null, timestamp);
	}
	
	@Override
	public String toString() {
		return String.format("(%s) %s: %s", timeStr, "SYSTEM", content); 
	}
	
	public String getMessage() {
		return content;
	}

	public Node getOrigin() {
		throw new UnsupportedOperationException("Can't get the origin from a SystemMessage");
	}
}
