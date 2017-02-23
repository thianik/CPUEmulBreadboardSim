package CPUTests.Instructions;
/*

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import sk.uniza.fri.cp.CPUEmul.CPU;
import sk.uniza.fri.cp.CPUEmul.Instruction;
import sk.uniza.fri.cp.CPUEmul.Program;
import sk.uniza.fri.cp.CPUEmul.enumInstructionsSet;

import java.util.ArrayList;


public class TestCPUInstructions {

    private CPU cpu;

    @BeforeAll
    void initCPU(){
        cpu = new CPU(null, null, false, false, false);
    }

    @BeforeEach
    void reset(){
        cpu.reset();
    }

    @Test
    void basicAdd() throws InterruptedException {
        Instruction instMVIa = new Instruction(enumInstructionsSet.MVI, "a", "1");
        Instruction instMVIb = new Instruction(enumInstructionsSet.MVI, "b", "1");
        Instruction inst = new Instruction(enumInstructionsSet.ADD, "a", "b");

        cpu.execute(instMVIa);
        cpu.execute(instMVIb);
        cpu.execute(inst);

        assertTrue(cpu.getRegA() == 2);
    }

}*/
