from idaapi import *
def getFuncAddr(fname):
    return LocByName(fname)
def judgeAduit(addr):
    MakeComm(addr,"### AUDIT HERE ###")
    SetColor(addr,CIC_ITEM,0x0000ff)
def flagCalls(funcname):
    count = 0
    fAddr = getFuncAddr(funcname)
    func = get_func(fAddr)
    print("faddr : " + hex(fAddr))
    if not func is None:
        fname = Name(func.startEA)
        items = FuncItems(func.startEA)
        for i in items:
            for xref in XrefsTo(i,0):
                if xref.type == fl_CN or xref.type == fl_CF:
                    count += 1
                    Message("%s[%d]     calls       0x%08x      from       ==>  %08x\n"%(fname,count,xref.frm,i))
                    judgeAduit(xref.frm)
                else:
                    Warning("No function named '%s' found at location %x"%(funcname,fAddr))
flagCalls("strcmp")
'''
if __name__ == '__main__':
    flagCalls("strcpy")
'''