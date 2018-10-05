/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting.alignment

import androidx.ui.lerpDouble
import androidx.ui.engine.text.TextDirection
import androidx.ui.toStringAsFixed
import androidx.ui.truncDiv

// / An offset that's expressed as a fraction of a [Size], but whose horizontal
// / component is dependent on the writing direction.
// /
// / This can be used to indicate an offset from the left in [TextDirection.ltr]
// / text and an offset from the right in [TextDirection.rtl] text without having
// / to be aware of the current text direction.
// /
// / See also:
// /
// /  * [Alignment], a variant that is defined in physical terms (i.e.
// /    whose horizontal component does not depend on the text direction).
class AlignmentDirectional(
        // / The distance fraction in the horizontal direction.
        // /
        // / A value of -1.0 corresponds to the edge on the "start" side, which is the
        // / left side in [TextDirection.ltr] contexts and the right side in
        // / [TextDirection.rtl] contexts. A value of 1.0 corresponds to the opposite
        // / edge, the "end" side. Values are not limited to that range; values less
        // / than -1.0 represent positions beyond the start edge, and values greater than
        // / 1.0 represent positions beyond the end edge.
        // /
        // / This value is normalized into a [Alignment.x] value by the [resolve]
        // / method.
    val start: Double,
        // / The distance fraction in the vertical direction.
        // /
        // / A value of -1.0 corresponds to the topmost edge. A value of 1.0
        // / corresponds to the bottommost edge. Values are not limited to that range;
        // / values less than -1.0 represent positions above the top, and values
        // / greater than 1.0 represent positions below the bottom.
        // /
        // / This value is passed through to [Alignment.y] unmodified by the
        // / [resolve] method.
    val y: Double

) : AlignmentGeometry() {

    init {
        assert(start != null)
        assert(y != null)
    }

    override val _x: Double = 0.0

    override val _start: Double = start

    override val _y: Double = y

    companion object {
        // / The top corner on the "start" side.
        val topStart = AlignmentDirectional(-1.0, -1.0)

        // / The center point along the top edge.
        // /
        // / Consider using [Alignment.topCenter] instead, as it does not need
        // / to be [resolve]d to be used.
        val topCenter = AlignmentDirectional(0.0, -1.0)

        // / The top corner on the "end" side.
        val topEnd = AlignmentDirectional(1.0, -1.0)

        // / The center point along the "start" edge.
        val centerStart = AlignmentDirectional(-1.0, 0.0)

        // / The center point, both horizontally and vertically.
        // /
        // / Consider using [Alignment.center] instead, as it does not need to
        // / be [resolve]d to be used.
        val center = AlignmentDirectional(0.0, 0.0)

        // / The center point along the "end" edge.
        val centerEnd = AlignmentDirectional(1.0, 0.0)

        // / The bottom corner on the "start" side.
        val bottomStart = AlignmentDirectional(-1.0, 1.0)

        // / The center point along the bottom edge.
        // /
        // / Consider using [Alignment.bottomCenter] instead, as it does not
        // / need to be [resolve]d to be used.
        val bottomCenter = AlignmentDirectional(0.0, 1.0)

        // / The bottom corner on the "end" side.
        val bottomEnd = AlignmentDirectional(1.0, 1.0)

        // / Linearly interpolate between two [AlignmentDirectional]s.
        // /
        // / If either is null, this function interpolates from [AlignmentDirectional.center].
        // /
        // / The `t` argument represents position on the timeline, with 0.0 meaning
        // / that the interpolation has not started, returning `a` (or something
        // / equivalent to `a`), 1.0 meaning that the interpolation has finished,
        // / returning `b` (or something equivalent to `b`), and values in between
        // / meaning that the interpolation is at the relevant point on the timeline
        // / between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
        // / 1.0, so negative values and values greater than 1.0 are valid (and can
        // / easily be generated by curves such as [Curves.elasticInOut]).
        // /
        // / Values for `t` are usually obtained from an [Animation<double>], such as
        // / an [AnimationController].
        fun lerp(
            a: AlignmentDirectional?,
            b: AlignmentDirectional?,
            t: Double
        ): AlignmentDirectional? {
            assert(t != null)
            if (a == null && b == null)
                return null
            if (a == null)
                return AlignmentDirectional(lerpDouble(0.0, b!!.start, t), lerpDouble(0.0, b.y, t))
            if (b == null)
                return AlignmentDirectional(lerpDouble(a.start, 0.0, t), lerpDouble(a.y, 0.0, t))
            return AlignmentDirectional(lerpDouble(a.start, b.start, t), lerpDouble(a.y, b.y, t))
        }

        fun _stringify(start: Double, y: Double): String {
            if (start == -1.0 && y == -1.0)
                return "AlignmentDirectional.topStart"
            if (start == 0.0 && y == -1.0)
                return "AlignmentDirectional.topCenter"
            if (start == 1.0 && y == -1.0)
                return "AlignmentDirectional.topEnd"
            if (start == -1.0 && y == 0.0)
                return "AlignmentDirectional.centerStart"
            if (start == 0.0 && y == 0.0)
                return "AlignmentDirectional.center"
            if (start == 1.0 && y == 0.0)
                return "AlignmentDirectional.centerEnd"
            if (start == -1.0 && y == 1.0)
                return "AlignmentDirectional.bottomStart"
            if (start == 0.0 && y == 1.0)
                return "AlignmentDirectional.bottomCenter"
            if (start == 1.0 && y == 1.0)
                return "AlignmentDirectional.bottomEnd"
            return "AlignmentDirectional(${start.toStringAsFixed(1)}, " +
                "${y.toStringAsFixed(1)})"
        }
    }

    override fun add(other: AlignmentGeometry): AlignmentGeometry {
        if (other is AlignmentDirectional)
            return this + other
        return super.add(other)
    }

    // / Returns the difference between two [AlignmentDirectional]s.
    operator fun minus(other: AlignmentDirectional): AlignmentGeometry {
        return AlignmentDirectional(start - other.start, y - other.y)
    }

    // / Returns the sum of two [AlignmentDirectional]s.
    operator fun plus(other: AlignmentDirectional): AlignmentDirectional {
        return AlignmentDirectional(start + other.start, y + other.y)
    }

    // / Returns the negation of the given [AlignmentDirectional].
    override operator fun unaryMinus(): AlignmentDirectional {
        return AlignmentDirectional(-start, -y)
    }

    // / Scales the [AlignmentDirectional] in each dimension by the given factor.
    override operator fun times(other: Double): AlignmentGeometry {
        return AlignmentDirectional(start * other, y * other)
    }

    // / Divides the [AlignmentDirectional] in each dimension by the given factor.
    override operator fun div(other: Double): AlignmentGeometry {
        return AlignmentDirectional(start / other, y / other)
    }

    // / Integer divides the [AlignmentDirectional] in each dimension by the given factor.
    override fun truncDiv(other: Double): AlignmentGeometry {
        return AlignmentDirectional(
                (start.truncDiv(other)).toDouble(),
                (y.truncDiv(other)).toDouble())
    }

    // / Computes the remainder in each dimension by the given factor.
    override operator fun rem(other: Double): AlignmentGeometry {
        return AlignmentDirectional(start % other, y % other)
    }

    override fun resolve(direction: TextDirection?): Alignment {
        assert(direction != null)

        return when (direction!!) {
            TextDirection.RTL -> Alignment(-start, y)
            TextDirection.LTR -> Alignment(start, y)
        }
    }

    override fun toString() = _stringify(start, y)
}