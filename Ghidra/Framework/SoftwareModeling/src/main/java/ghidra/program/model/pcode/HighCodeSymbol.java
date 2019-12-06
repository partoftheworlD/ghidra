/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.program.model.pcode;

import ghidra.program.database.symbol.CodeSymbol;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressFactory;
import ghidra.program.model.data.DataType;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolUtilities;
import ghidra.util.exception.InvalidInputException;
import ghidra.xml.XmlElement;
import ghidra.xml.XmlPullParser;

/**
 * Symbol returned by the decompiler that wraps a CodeSymbol
 */
public class HighCodeSymbol extends HighSymbol {

	private CodeSymbol symbol;
	private Data data;
	private VariableStorage storage;

	public HighCodeSymbol(CodeSymbol sym, DataType dataType, int sz, HighFunction func) {
		super(sym.getID(), sym.getName(), dataType, sz, null, func);
		symbol = sym;
		data = null;
	}

	public HighCodeSymbol(long id, Address addr, DataType dataType, int sz, HighFunction func) {
		super(id, SymbolUtilities.getDynamicName(func.getFunction().getProgram(), addr), dataType,
			sz, null, func);
		symbol = null;
		data = func.getFunction().getProgram().getListing().getDataAt(addr);
	}

	@Override
	public boolean isGlobal() {
		return true;
	}

	public CodeSymbol getCodeSymbol() {
		return symbol;
	}

	public Data getData() {
		if (data == null) {
			Object dataObj = symbol.getObject();
			if (dataObj instanceof Data) {
				data = (Data) dataObj;
			}
		}
		return data;
	}

	@Override
	public VariableStorage getStorage() {
		if (storage == null) {
			Data dataObj = getData();
			if (dataObj != null) {
				try {
					storage = new VariableStorage(function.getFunction().getProgram(),
						dataObj.getAddress(), dataObj.getLength());
				}
				catch (InvalidInputException e) {
					storage = VariableStorage.UNASSIGNED_STORAGE;
				}
			}
			else {
				storage = VariableStorage.UNASSIGNED_STORAGE;
			}
		}
		return storage;
	}

	@Override
	public String buildXML() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void restoreXML(XmlPullParser parser) throws PcodeXMLException {
		XmlElement symel = parser.start("symbol");
		restoreSymbolXML(symel);
		Symbol tmpSymbol = function.getFunction().getProgram().getSymbolTable().getSymbol(getId());
		if (tmpSymbol instanceof CodeSymbol) {
			symbol = (CodeSymbol) tmpSymbol;
		}
		type = function.getDataTypeManager().readXMLDataType(parser);
		size = type.getLength();
		parser.end(symel);
		restoreEntryXML(parser);
		while (parser.peek().isStart()) {
			parser.discardSubTree();
		}
	}

	@Override
	protected void restoreEntryXML(XmlPullParser parser) throws PcodeXMLException {
		AddressFactory addrFactory = function.getAddressFactory();

		XmlElement addrel = parser.start("addr");
		int sz = type.getLength();
		if (sz == 0) {
			throw new PcodeXMLException("Invalid symbol 0-sized data-type: " + type.getName());
		}
		Address varAddr = Varnode.readXMLAddress(addrel, addrFactory);
		parser.end(addrel);
		pcaddr = parseRangeList(parser);
		if (symbol == null) {
			Program program = function.getFunction().getProgram();
			data = program.getListing().getDataAt(varAddr);
		}
	}
}
