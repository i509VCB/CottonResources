/*
 * MIT License
 *
 * Copyright (c) 2018-2020 The Cotton Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.cottonmc.resources.world.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.google.common.primitives.Floats;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class Clump {
	protected ArrayList<Entry> entries = new ArrayList<>();

	public BlockPos removeUniform(Random rand, int dx, int dy, int dz) {
		int index = rand.nextInt(entries.size());
		return entries.remove(index).blockPos(dx, dy, dz);
	}

	public BlockPos removeGaussian(Random rand, int dx, int dy, int dz) {
		int index = (int) (Math.abs(rand.nextGaussian() * entries.size()));
		if (index >= entries.size()) index = 0; //in unlikely scenarios, gaussian numbers can go way outside the bounds.
		if (index < 0) index = 0;

		return entries.remove(index).blockPos(dx, dy, dz);
	}

	public Clump copy() {
		Clump result = new Clump();
		result.entries.addAll(this.entries);
		return result;
	}

	public static Clump of(float r) {
		int ir = (int) Math.ceil(r);
		Clump result = new Clump();

		for (int z = -ir; z <= ir; z++) {
			for (int x = -ir; x <= ir; x++) {
				for (int y = -ir; y <= ir; y++) {
					float d = (float) Math.sqrt(x * x + y * y + z * z);
					if (d <= r) result.entries.add(new Entry(x, y, z, d));
				}
			}
		}

		if (result.entries.isEmpty()) result.entries.add(new Entry(0, 0, 0, 0));

		Collections.sort(result.entries, (a, b) -> Floats.compare(a.d, b.d));

		return result;
	}

	public int size() {
		return entries.size();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	private static class Entry {
		public float d;
		public int x;
		public int y;
		public int z;

		private Entry(int x, int y, int z, float d) {
			this.d = d;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Vec3i pos() {
			return new Vec3i(x, y, z);
		}

		public BlockPos blockPos(int x, int y, int z) {
			return new BlockPos(this.x + x, this.y + y, this.z + z);
		}
	}
}
